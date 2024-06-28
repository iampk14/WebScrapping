package com.eulerity.hackathon.imagefinder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageScrapper {
    private String URL;
    private static List<String> imageUrls = new ArrayList<>();

    public ImageScrapper(String URL) {
        this.URL = URL;
        try {
            // Fetch the HTML document
            Document doc = Jsoup.connect(this.URL)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .timeout(12000)
                    .followRedirects(true)
                    .get();

            // Extract all image elements
            Elements images = doc.select("img");

            // List to store image URLs
           

            // Iterate over the image elements and extract src attribute
            for (Element image : images) {
                String imageUrl = image.absUrl("src");
                if (!imageUrl.isEmpty()) {
                    imageUrls.add(imageUrl);
                }
            }

            // Print all image URLs (for demonstration)
            for (String imageUrl : imageUrls) {
                System.out.println("Image URL: " + imageUrl);
            }

        } catch (IOException e) {
            // Handle any IOException that occurs during Jsoup connection
            e.printStackTrace();
        }
        
    }
    public List<String> getData(){
        return imageUrls;
    }
}