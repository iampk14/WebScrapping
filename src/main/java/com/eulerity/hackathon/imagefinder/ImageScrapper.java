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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageScrapper implements Runnable {
    private String URL;
    private static List<String> imageUrls = new ArrayList<>();
    private static Set<String> visitedUrls = new HashSet<>();
    private static final int MAX_DEPTH = 3;
    private static final int MAX_LINKS = 10;
    private int validLinkCount = 0;
    private String domain;

    @Override
    public void run() {

    }

    public ImageScrapper(String URL, int depth) {
        this.URL = URL;
        if (depth <= MAX_DEPTH) {
            scrapeImages(URL, depth);
        }
    }

    private void scrapeImages(String URL, int depth) {
        if (visitedUrls.contains(URL) || validLinkCount >= MAX_LINKS) {
            return;
        }
        visitedUrls.add(URL);

        try {
            // Fetch the HTML document
            Document doc = Jsoup.connect(URL)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .timeout(12000)
                    .followRedirects(true)
                    .get();

            // Extract all image elements
            domain = getDomainName(URL);
            getImages(doc);

            // Find links to other pages and recursively scrape them
            if (depth < MAX_DEPTH && validLinkCount < MAX_LINKS) {
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    if (validLinkCount >= MAX_LINKS) {
                        break;
                    }
                    String nextUrl = link.absUrl("href");
                    if (!nextUrl.isEmpty() && !visitedUrls.contains(nextUrl) && domain.equals(getDomainName(nextUrl)) ){
                        validLinkCount++;
                        scrapeImages(nextUrl, depth + 1);
                    }
                }
            }

        } catch (HttpStatusException e) {
            // Handle specific HTTP status errors
            System.err.println("HTTP error fetching URL. Status=" + e.getStatusCode() + ", URL=" + e.getUrl());
        } catch (IOException e) {
            // Handle any IOException that occurs during Jsoup connection
            System.out.println("IOException occurred: " + e.getMessage());
        } catch (Exception e) {
            // Handle any other exceptions
            System.out.println("Exception has occurred: " + e.getMessage());
        }
    }

    private void getImages(Document doc) throws URISyntaxException {
        Elements images = doc.select("img");
        // Iterate over the image elements and extract src attribute
        for (Element image : images) {
            String imageUrl = image.absUrl("src");
            if (!imageUrl.isEmpty() && !imageUrls.contains(imageUrl) && domain.equals(getDomainName(imageUrl))) {
                imageUrls.add(imageUrl);
            }
        }
    }

    private static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public List<String> getData() {
        // for (String s : imageUrls) {
        //     System.out.println(s);
        // }
        return imageUrls;
    }

    public boolean reset() {
        imageUrls.clear();
        visitedUrls.clear();
        validLinkCount = 0;
        return true;
    }

}
