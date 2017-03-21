import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Console;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {
    public static Logger log = Logger.getLogger(Main.class.getSimpleName());

    static class ProducerThread extends Thread{
        @Override
        public void run() {
            while(!shouldStop) {
                try {
                    String url = urlQueue.remove();
                    documentsQueue.put(download(url));

                    processedUrl.add(url);
                    System.out.println(url);
                    System.out.println(Thread.currentThread().getId());

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (NoSuchElementException e){

                }
            }
            super.run();
        }
    }

    static class ConsumerThread extends Thread{
        @Override
        public void run() {
            while(!shouldStop) {
                try {
                    Document doc = documentsQueue.poll(5, TimeUnit.SECONDS);
                    List<String> urls = extractLinks(doc);
                    for (String url : urls) {
                        if (url.startsWith(domain) || (!processedUrl.contains(url))) {
                            urlQueue.put(url);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    shouldStop = true;
                    System.out.println("STOP IT");
                }
            }
            super.run();
        }
    }

    public static BlockingQueue<String> urlQueue;
    public static Set<String> processedUrl;
    public static BlockingQueue<Document> documentsQueue;
    public static String domain;
    public static boolean shouldStop = false;

    public static void main(String[] args) {
        int n = 10;
        int m = 10;
        domain = "http://isu.ru";

        //todo Создать блокирующую очередь для URL (???Queue<String>)
        urlQueue = new ArrayBlockingQueue<String>(n);


        //todo Создать множество обработанных ссылок (Set<String>)

        processedUrl = new HashSet<String>(n);

        //todo Создать блокирующую очередь для документов ( ???Queue<Document>)

        documentsQueue = new ArrayBlockingQueue<Document>(n);

        //Добавить в очередь для URL адрес домена для скачивания, например, http://isu.ru
        urlQueue.add(domain);
        System.out.println(Thread.currentThread().getId());

        /* Producer
        todo Запустить n потоков-производителей, которые будут брать URL из очереди, скачивать и класть Document в очередь документов.
        todo По завершении добавлять обработанный URL в множество обработанных ссылок.
        todo Занести обработанный URL в журнал (вывести на консоль)
        */
        for (int i = 0; i < n; i++){
            ProducerThread p = new ProducerThread();
            p.start();
        }

        /* Consumer
        todo Запустить m потоков-потребителей, которые будут брать Document из очереди, получать список URL из него.
        todo Отфильтровывать только те, что начинаются с указанного домена и не входят в множество обработанных гиперссылок
        todo Добавить полученный отфильтрованный список URL в очередь
        */
        for (int i = 0; i < m; i++){
            ConsumerThread c = new ConsumerThread();
            c.start();
        }

    }


    public static void demo() {
        try {
            String domain = "http://isu.ru";
            //produce: получаем документ по URL
            Document doc = download(domain);
            //consume: получаем ссылки из документа
            List<String> links = extractLinks(doc);
            for (String link : links) {
                log.info(link);
            }
        } catch (IOException e) {
            log.severe("Can not download url+ " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public static Document download(String url) throws IOException {
        Document doc;
        Connection connect = Jsoup.connect(url);
        //todo прописать прокси-сервер, если выполняем в ISU
        //connect.proxy("http://proxy.isu.ru",3128);
        doc = connect.get();
        return doc;
    }

    public static List<String> extractLinks(Document doc) {
        //Список элементов, содержащих гиперссылки (тэги <a> с атрибутом href)
        Elements els = doc.select("a[href]");
        List<String> ret = new ArrayList<String>();
        for (Element el : els) {
            //Получаем значение атрибута href. Опция abs:href возвращает абослютный путь
            ret.add(el.attr("abs:href"));
        }
        return ret;
    }
}
