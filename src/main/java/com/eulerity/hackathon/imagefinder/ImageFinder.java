package com.eulerity.hackathon.imagefinder;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * ImageFinder Servlet
 * 
 * This servlet handles image scraping requests for the Eulerity Hackathon Challenge.
 * It processes POST requests to scrape images from a given URL using the ImageScrapper class.
 * 
 * @author Prasad Kulkarni
 * @version 1.0
 */
@WebServlet(name = "ImageFinder", urlPatterns = { "/main" })
public class ImageFinder extends HttpServlet {
    
    /** Serialization ID for the servlet */
    private static final long serialVersionUID = 1L;
    
    /** GSON instance for JSON processing */
    protected static final Gson GSON = new GsonBuilder().create();

    /** 
     * Sample test images array
     * Used when no URL is provided in the request
     */
    public static final String[] testImages = {
            "https://images.pexels.com/photos/545063/pexels-photo-545063.jpeg?auto=compress&format=tiny",
            "https://images.pexels.com/photos/464664/pexels-photo-464664.jpeg?auto=compress&format=tiny",
            "https://images.pexels.com/photos/406014/pexels-photo-406014.jpeg?auto=compress&format=tiny",
            "https://images.pexels.com/photos/1108099/pexels-photo-1108099.jpeg?auto=compress&format=tiny"
    };

    /**
     * Handles POST requests to the servlet.
     * Processes the URL parameter, initiates image scraping, and returns the results as JSON.
     * 
     * @param req The HTTP request object
     * @param resp The HTTP response object
     * @throws ServletException If the request cannot be handled
     * @throws IOException If an input or output error occurs
     */
    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Reset the scrapper state for each new request
        ImageScrapper.reset();

        // Set response content type to JSON
        resp.setContentType("application/json");

        String path = req.getServletPath();
        String url = req.getParameter("url");

        if (url != null && !url.isEmpty()) {
            try {
                // Initialize and run the ImageScrapper
                ImageScrapper imageScrapper = new ImageScrapper(url, 0);
                imageScrapper.getThread().join();

                // Log the request details
                System.out.println("Got request of: " + path + " with query param: " + url);

                // Write the scraped image URLs as JSON response
                resp.getWriter().print(GSON.toJson(ImageScrapper.getData()));
            } catch (Exception e) {
                // Log the exception and send an error response
                e.printStackTrace();
                resp.getWriter().print(GSON.toJson("Error occurred while processing the request."));
            }
        } else {
            // If no URL is provided, return the test images
            resp.getWriter().print(GSON.toJson(testImages));
        }
    }
}