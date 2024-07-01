package com.eulerity.hackathon.imagefinder;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageScrapper implements Runnable {
    private String URL;
    private static List<String> imageUrls = Collections.synchronizedList(new ArrayList<>());
    private static Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
    private static final int MAX_DEPTH = 2;
    private static final int MAX_LINKS = 100;
    private static volatile int validLinkCount = 0;
    private String domain;
    private int depth;
    private Thread thread;
    private static List<Thread> runningThreads = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void run() {
        scrapeImages(URL, depth);
    }

    public ImageScrapper(String URL, int depth) {
        this.URL = URL;
        this.depth = depth;
        this.domain = getDomainName(URL);
        if (depth <= MAX_DEPTH) {
            this.thread = new Thread(this);
            runningThreads.add(this.thread);
            this.thread.start();
        }
    }

    public Thread getThread() {
        return this.thread;
    }

    private void scrapeImages(String URL, int depth) {
        if (visitedUrls.contains(URL) || validLinkCount >= MAX_LINKS) {
            return;
        }
        visitedUrls.add(URL);

        try {
            Document doc = Jsoup.connect(URL)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .timeout(50000)
                    .followRedirects(true)
                    .get();

            getImages(doc);

            if (depth < MAX_DEPTH && validLinkCount < MAX_LINKS) {
                Elements links = doc.select("a[href]");
                List<Thread> threads = new ArrayList<>();
                for (Element link : links) {
                    if (validLinkCount >= MAX_LINKS) {
                        break;
                    }
                    String nextUrl = link.absUrl("href");
                    if (isValidUrl(nextUrl) && !visitedUrls.contains(nextUrl) && domain.equals(getDomainName(nextUrl))) {
                        synchronized (this) {
                            validLinkCount++;
                        }
                        Thread thread = new Thread(new ImageScrapper(nextUrl, depth + 1));
                        threads.add(thread);
                        thread.start();
                    }
                }
                for (Thread thread : threads) {
                    thread.join();
                }
            }

        } catch (HttpStatusException e) {
            System.err.println("HTTP error fetching URL. Status=" + e.getStatusCode() + ", URL=" + e.getUrl());
        } catch (IOException e) {
            System.out.println("IOException occurred: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception has occurred: " + e.getMessage());
        }
    }

    private void getImages(Document doc) {
        Elements images = doc.select("img");
        for (Element image : images) {
            String imageUrl = image.absUrl("src");
            if (isValidUrl(imageUrl) && !imageUrls.contains(imageUrl) && domain.equals(getDomainName(imageUrl))) {
                imageUrls.add(imageUrl);
            }
        }
    }

    private boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null && (host.endsWith(".com") || host.endsWith(".org") || host.endsWith(".in"));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static String getDomainName(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain != null && domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException e) {
            return "";
        }
    }

    public static List<String> getData() {
        return new ArrayList<>(imageUrls);
    }

    public static void reset() {
        for (Thread thread : runningThreads) {
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }
        runningThreads.clear();
        imageUrls.clear();
        visitedUrls.clear();
        validLinkCount = 0;
    }
}
