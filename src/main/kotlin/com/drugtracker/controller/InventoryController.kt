package com.drugtracker.controller
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import com.drugtracker.service.InventoryService
import com.drugtracker.service.CompoundService
import org.springframework.web.bind.annotation.*

/**
 * Controller responsible for handling all HTTP requests related to Inventory management.
 *
 * This controller allows users to:
 * - View all inventory records
 * - Delete inventory entries
 *
 * It follows the MVC architecture:
 * - Controller: Handles incoming HTTP requests
 * - Service Layer: Contains business logic
 * - Model: Transfers data to the view layer
 * - View: Thymeleaf templates render the HTML interface
 *
 * Base URL mapping: /inventory
 *
 * @property service Handles inventory-related business logic
 * @property compoundService Provides compound-related data if needed
 */
@Controller
@RequestMapping("/inventory")
class InventoryController(
    private val service: InventoryService,
    private val compoundService: CompoundService
) {
    /**
     * Displays a list of all inventory records.
     *
     * URL: GET /inventory
     *
     * Retrieves inventory data from the service layer and
     * adds it to the model so it can be displayed in the
     * inventory view template.
     *
     * @param model Spring UI model used to pass attributes to the view
     * @return Logical view name for the inventory list page
     */
    @GetMapping
    fun list(model: Model): String {
        // Fetch all inventory records from the service layer
        model.addAttribute("inventory", service.getAll())
        // Return the Thymeleaf template name (without .html extension)
        return "inventory"
    }

    /**
     * Deletes an inventory entry by its database ID.
     *
     * URL: GET /inventory/delete/{id}
     *
     * Delegates deletion logic to the service layer and
     * redirects the user back to the inventory list page.
     *
     * @param id Database ID of the inventory record to delete
     * @return Redirect to inventory list page (Post/Redirect/Get pattern)
     */
    @GetMapping("/delete/{id}")
    fun delete(@PathVariable id: Long): String {
        // Call service layer to remove the inventory entry
        service.delete(id)
        // Redirect back to inventory overview page
        return "redirect:/inventory"
    }
}