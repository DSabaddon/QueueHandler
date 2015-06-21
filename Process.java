package org.MDS;

import java.util.logging.Level;
import java.util.logging.Logger;

// Процесс, выполняющий работу (в данном случае "печать на экран")
/* Почему наследник, а не реализатор интерфейса?
Когда процессы закончили работу, они не нужны как объекты, а когда диспетчер закончил, его можно юзать дальше*/
public class Process extends Thread
{
    private ProcessDispatcher dispatcher; // Ссылка на "хозяина"
    private int groupNum; // Номер группы для обработки

    public Process(ProcessDispatcher dispatcher, String name) {
        super(name);
        this.dispatcher = dispatcher;
        groupNum = -1;
    }

    // Метод, позволяющий назначить процессу работу
    public void setWork(int groupNum)
    {
        this.groupNum = groupNum;
        QueueHandler.showMessage(this.getName() + " назначен на группу " + groupNum);
        synchronized(this)
        {
            this.notifyAll();
        }
    }
    
    // Выполнение работы
    private void doWork()
    {
        Element element = dispatcher.giveElement(groupNum); // Пытаемся получить элемент для обработки и заблокировать группу
        QueueHandler.showMessage("ищет элемент в группе " + groupNum);
        if (element != null)
        {
            dispatcher.elementProcessed(groupNum); // Если элемент был найден и удалось заблокировать группу, то обрабатываем его
            QueueHandler.showMessage("! itemID = " + element.getItemID() + "; groupID = " + element.getGroupID());
        }
        groupNum = -1; // Ставим флаг "отсутствие работ"
    }

    @Override
    public void run() {
        QueueHandler.showMessage("начал работу");
        while(true)
        {
            if (dispatcher.askForWork(this)) // Если есть ещё работа, то встаём в очередь и ждём
            {
                synchronized(this)
                {
                    try
                    {
                        while (groupNum == -1) // Цикл нужен для защиты от лишних (ранних или запоздавших) срабатываний метода notify у диспетчера
                        {
                            this.wait(); // Когда диспетчер даст работу, он разбудит
                        }
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                doWork();
            }
            else
            {
                break;
            }
        }
        QueueHandler.showMessage("закончил работу");
    }
}
