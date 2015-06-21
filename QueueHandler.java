package org.MDS;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
  * Если вкратце, то принцип следующий.
  * Есть очередь, которая заполняется внешним процессом. В условии задачи про очередь ничего не сказано, поэтому используется обычная очередь.
  * С такой очередью работать сложно, так как нам нужна разюивка элементов по группам. Основной процесс перерабатывает исхоную очередь в "удобную очередь".
  * "Удобная очередь" представляет собой хэшмап очередей. То есть мы имеем несколько очередей и в каждой очереди хранятся элементы, принадлежащие одной группе.
  * К тому же, эти очереди реализованы так, что каждую можно блокировать.
  * Далее, есть процесс-Диспетчер, у которого есть очередь свободных процессов.
  * Диспетчер постоянно просматривает эту очередь, берет свободный процесс и даёт ему на обработку следующую группу (группы перебираются поочереди, диспетчер знает какую группу давал последней).
  * Процессы постоянно ожидают работы. Как только работа назначается, процесс просыпается, пытается сделать работу и снова засыпает, встав в очередь свободных процессов.
  * "Пытается сделать работу" в предыдущем предложении означает вот что: процессу дали группу и он проверяет, а есть ли там вообще элементы для обработки и не заблокирована ли она.
  * Если всё ОК, то процесс обрабатывает один элемент.
 * @author Dmitry
 */
public class QueueHandler {
    public static void showMessage(String s)
    {
        System.out.println("(" + System.currentTimeMillis() + ")-[" + Thread.currentThread().getName() + "] : " + s);
    }
    
    private static LinkedBlockingQueue<Element> queue;
    private static QueueFiller filler;
    private static ProcessDispatcher dispatcher;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        Thread.currentThread().setName("MainThread");
        QueueHandler.showMessage("начал работу");
        // Значения исходных данных по умолчанию
        int amountOfElements = 100; // Количество элементов
        int amountOfGroups = 5; // Количество групп
        int amountOfProcesses = 3; // Количество процессов
        if (args.length == 3)
        {
            amountOfElements = Integer.parseInt(args[0]);
            amountOfGroups = Integer.parseInt(args[1]);
            amountOfProcesses = Integer.parseInt(args[2]);
        }
        queue = new LinkedBlockingQueue<>(); // Очередь элементов
        filler = new QueueFiller(queue, amountOfElements, amountOfGroups); // Внешний процесс для заполнения очереди
        filler.start();
        
        dispatcher = new ProcessDispatcher(amountOfElements, amountOfGroups, amountOfProcesses); // Диспетчер процессов
        Thread dispatcherThread = dispatcher.start();

        // Пока внешний процесс не закончил работу или очередь не пуста, основной процесс пытается перерабатывать очередь
        while (!filler.isFinished() || !queue.isEmpty())
        {
            try {
                Element element = queue.poll(2, TimeUnit.SECONDS);
                if (element != null)
                {
                    showMessage("- itemID = " + element.getItemID() + "; groupID = " + element.getGroupID());
                    dispatcher.addToQueue(element);
                }
                else
                {
                    showMessage("очередь кончилась");
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(QueueHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Ждём окончания работы диспетчера
        try
        {
            dispatcherThread.join();
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(QueueHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        QueueHandler.showMessage("закончил работу");
    }
}
