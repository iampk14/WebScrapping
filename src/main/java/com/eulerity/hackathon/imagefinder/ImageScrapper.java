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

/**
 * ImageScrapper class
 * 
 * This class is responsible for scraping images from web pages.
 * It implements multi-threading to improve performance and implements
 * depth-limited crawling to avoid excessive resource usage.
 */
public class ImageScrapper implements Runnable {
    // The URL to scrape
    private String URL;
    
    // Thread-safe list to store found image URLs
    private static List<String> imageUrls = Collections.synchronizedList(new ArrayList<>());
    
    // Thread-safe set to keep track of visited URLs
    private static Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
    
    // Constants to limit crawling depth and number of links
    private static final int MAX_DEPTH = 2;
    private static final int MAX_LINKS = 100;
    
    // Counter for valid links processed
    private static volatile int validLinkCount = 0;
    
    // Domain of the URL being scraped
    private String domain;
    
    // Current depth of crawling
    private int depth;
    
    // Thread for this scraper instance
    private Thread thread;
    
    // List of all running threads
    private static List<Thread> runningThreads = Collections.synchronizedList(new ArrayList<>());

    /**
     * Implements the Runnable interface.
     * Initiates the image scraping process.
     */
    @Override
    public void run() {
        scrapeImages(URL, depth);
    }

    /**
     * Constructor for ImageScrapper
     * 
     * @param URL The URL to scrape
     * @param depth The current depth of crawling
     */
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

    /**
     * Getter for the thread of this scraper instance
     * 
     * @return The Thread object
     */
    public Thread getThread() {
        return this.thread;
    }

    /**
     * Main method for scraping images from a URL
     * 
     * @param URL The URL to scrape
     * @param depth The current depth of crawling
     */
    private void scrapeImages(String URL, int depth) {
        if (visitedUrls.contains(URL) || validLinkCount >= MAX_LINKS) {
            return;
        }
        visitedUrls.add(URL);

        try {
            // Connect to the URL and get the HTML document
            Document doc = Jsoup.connect(URL)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .timeout(50000)
                    .followRedirects(true)
                    .get();

            // Extract images from the document
            getImages(doc);

            // If not at max depth and not exceeded max links, crawl further
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
                // Wait for all spawned threads to complete
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

    /**
     * Extracts image URLs from the HTML document
     * 
     * @param doc The JSoup Document object
     */
    private void getImages(Document doc) {
        Elements images = doc.select("img");
        for (Element image : images) {
            String imageUrl = image.absUrl("src");
            if (isValidUrl(imageUrl) && !imageUrls.contains(imageUrl) && domain.equals(getDomainName(imageUrl))) {
                imageUrls.add(imageUrl);
            }
        }
    }

    /**
     * Checks if a URL is valid
     * 
     * @param url The URL to check
     * @return true if the URL is valid, false otherwise
     */
    private boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null && (host.endsWith(".com") || host.endsWith(".org") || host.endsWith(".in"));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Extracts the domain name from a URL
     * 
     * @param url The URL to extract the domain from
     * @return The domain name
     */
    private static String getDomainName(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain != null && domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException e) {
            return "";
        }
    }

    /**
     * Returns the list of scraped image URLs
     * 
     * @return A new ArrayList containing all scraped image URLs
     */
    public static List<String> getData() {
        return new ArrayList<>(imageUrls);
    }

    /**
     * Resets the scraper state
     * Interrupts all running threads and clears all data structures
     */
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