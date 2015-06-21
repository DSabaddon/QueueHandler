package org.MDS;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

// Внешний процесс, заполняющий очередь
public class QueueFiller
implements Runnable
{
    private BlockingQueue<Element> queue; // Очередь для заполнения
    private int amountOfElements; // Количество элементов
    private int amountOfGroups; // Количество групп
    private boolean finished; // Признак окончания работ данного процесса

    public QueueFiller(BlockingQueue<Element> queue, int amountOfElements, int amountOfGroups) {
        this.queue = queue;
        this.amountOfElements = amountOfElements;
        this.amountOfGroups = amountOfGroups;
        finished = false;
    }

    public boolean isFinished() {
        return finished;
    }

    @Override
    public void run() {
        QueueHandler.showMessage("начал работу");
        finished = false;
        Random rnd = new Random();
        int group;
        for (int i = 1; i <= amountOfElements; i++) {
            try {
                group = rnd.nextInt(amountOfGroups) + 1;
                queue.offer(new Element(i, group));
                QueueHandler.showMessage("+ itemID = " + i + "; groupID = " + group);
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                Logger.getLogger(QueueFiller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        finished = true;
        QueueHandler.showMessage("закончил работу");
    }
    
    // Создание потока и запуск процесса заполнения
    public void start()
    {
        Thread th = new Thread(this, "QueueFiller");
        th.start();
    }
}
