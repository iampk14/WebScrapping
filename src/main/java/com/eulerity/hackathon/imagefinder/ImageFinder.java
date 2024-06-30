package com.eulerity.hackathon.imagefinder;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@WebServlet(name = "ImageFinder", urlPatterns = { "/main" })
public class ImageFinder extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected static final Gson GSON = new GsonBuilder().create();

    // This is just a test array
    public static final String[] testImages = {
            "https://images.pexels.com/photos/545063/pexels-photo-545063.jpeg?auto=compress&format=tiny",
            "https://images.pexels.com/photos/464664/pexels-photo-464664.jpeg?auto=compress&format=tiny",
            "https://images.pexels.com/photos/406014/pexels-photo-406014.jpeg?auto=compress&format=tiny",
            "https://images.pexels.com/photos/1108099/pexels-photo-1108099.jpeg?auto=compress&format=tiny"
    };

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String path = req.getServletPath();
        String url = req.getParameter("url");
        
        if (url != null && !url.isEmpty()) {
            try {
                // Instantiate a new ImageScrapper for each request
                ImageScrapper imageScrapper = new ImageScrapper(url, 0);

                // Wait for the scraping to complete
                imageScrapper.getThread().join();

                System.out.println("Got request of: " + path + " with query param: " + url);

                // Send the scraped image URLs as JSON response
                resp.getWriter().print(GSON.toJson(ImageScrapper.getData()));

                // Reset the scrapper state for the next request
                ImageScrapper.reset();
            } catch (Exception e) {
                e.printStackTrace();
                resp.getWriter().print(GSON.toJson("Error occurred while processing the request."));
            }
        } else {
            // If no URL parameter is provided, return the test images
            resp.getWriter().print(GSON.toJson(testImages));
        }
    }
}
