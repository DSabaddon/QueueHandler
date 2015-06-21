package org.MDS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

// Диспетчер процессов
public class ProcessDispatcher
implements Runnable
{
    // Очередь с возможностью блокировки
    private class LockableQueue<Type> extends LinkedBlockingQueue<Type>
    {
        private ReentrantLock lock;

        public LockableQueue() {
            super();
            lock = new ReentrantLock();
        }
    }

    private int amountOfElements; // Количество элементов
    private int amountOfGroups; // Количество групп
    private int amountOfProcesses; // Количество процессов
    private int amountOfProcessedElements; // Количество обработанных элементов
    private boolean workIsOver; // Признак, что работы заверщены
    private ArrayList<Process> processes; // Массив процессов
    private LinkedBlockingQueue<Process> idleProcessQueue; // Очередь свободных процессов
    private HashMap<Integer, LockableQueue<Element>> groupQueues; // Хэшмап очередей с элементами

    public ProcessDispatcher(int amountOfElements, int amountOfGroups, int amountOfProcesses) {
        this.amountOfElements = amountOfElements;
        this.amountOfGroups = amountOfGroups;
        this.amountOfProcesses = amountOfProcesses;
        amountOfProcessedElements = 0;
        processes = new ArrayList<>();
        idleProcessQueue = new LinkedBlockingQueue<>();
        groupQueues = new HashMap<>(amountOfGroups + 1, 1);
    }

    // Занесение элемента в хешмап очередей
    public void addToQueue(Element element)
    {
        int key = element.getGroupID();
        LockableQueue<Element> curQueue = groupQueues.get(key);
        if (curQueue == null)
        {
            curQueue = new LockableQueue<>();
            groupQueues.put(key, curQueue);
        }
        curQueue.offer(element);
        QueueHandler.showMessage("* itemID = " + element.getItemID() + "; groupID = " + element.getGroupID());
    }
    
    // Просьба предоставить работу.
    public boolean askForWork(Process process)
    {
        if (!workIsOver) // Если работа ещё есть, то процесс встанет в очередь на ожидание работы
        {
            QueueHandler.showMessage("ждёт работу");
            idleProcessQueue.offer(process);
            return true;
        }
        else
        {
            return false;
        }
    }
    
    // Найти элемент и заблокировать группу
    public Element giveElement(int groupNum)
    {
        LockableQueue<Element> curQueue = groupQueues.get(groupNum);
        if (curQueue != null)
        {
            if (!curQueue.isEmpty() && curQueue.lock.tryLock())
            {
                return curQueue.poll();
            }
        }
        return null;
    }
    
    // Увеличить счетчик обработанных элементов и разблокировать группу
    public void elementProcessed(int groupNum)
    {
        LockableQueue<Element> curQueue = groupQueues.get(groupNum);
        curQueue.lock.unlock();

        synchronized(this)
        {
            amountOfProcessedElements++;
            if (amountOfProcessedElements == amountOfElements)
            {
                workIsOver = true;
            }
        }
    }

    @Override
    public void run() {
        QueueHandler.showMessage("начал работу");
        amountOfProcessedElements = 0;
        workIsOver = false;
        Process curProcess;
        int curGroup = 0;

        for (int i = 1; i <= amountOfProcesses; i++) // Создание(запуск) процессов
        {
            curProcess = new Process(this, "Process №" + i);
            processes.add(curProcess);
            curProcess.start();
        }

        while (!workIsOver) // Пока есть работа, анализируется очередь процессов и каждому выдается следующая по номеру группа для обработки
        {
            try
            {
                curProcess = idleProcessQueue.poll(3, TimeUnit.SECONDS);
                if (curProcess != null)
                {
                    curGroup = (curGroup % amountOfGroups) + 1;
                    curProcess.setWork(curGroup);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ProcessDispatcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (Process process : processes) // Ожидание завершения всех процессов
        {
            synchronized(process)
            {
                try
                {
                    if (process.getState() == Thread.State.WAITING) process.setWork(0); // Если так случилось, что какой-то процесс ждёт - разбудить его
                    process.join();
                } catch (InterruptedException ex)
                {
                    Logger.getLogger(ProcessDispatcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        QueueHandler.showMessage("закончил работу");
    }
    
    // Запуск диспетчера
    public Thread start()
    {
        Thread th = new Thread(this, "ProcessDispatcher");
        th.start();
        return th;
    }
}
