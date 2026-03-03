package com.drugtracker.controller

import com.drugtracker.service.CompoundService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

/**
 * Controller responsible for handling requests to the application's main dashboard.
 *
 * This serves as the entry point of the DrugTracker web application.
 * It retrieves aggregated statistical data from the service layer
 * and displays it on the dashboard view.
 *
 * Base URL mapping: "/"
 *
 * @property compoundService Service layer responsible for retrieving
 *                            compound-related data and dashboard statistics
 */
@Controller
class IndexController(
    private val compoundService: CompoundService
) {
    /**
     * Displays the main dashboard page.
     *
     * URL: GET /
     *
     * Retrieves aggregated statistics (such as total compounds,
     * active compounds, clinical trial counts, etc.) from the service layer
     * and adds them to the model so they can be rendered in the dashboard view.
     *
     * This method follows the MVC pattern:
     * - Controller: Handles HTTP request
     * - Service: Provides business logic and aggregated data
     * - Model: Transfers data to the view
     * - View: Thymeleaf template renders the dashboard UI
     *
     * @param model Spring UI model used to pass attributes to the view
     * @return Logical view name for the dashboard page
     */
    @GetMapping("/")
    fun dashboard(model: Model): String {
        // Retrieve dashboard statistics from the service layer
        model.addAttribute("stats", compoundService.getDashboardStats())
        // Return the Thymeleaf template name (without .html extension)
        return "dashboard"
    }
}