package com.drugtracker.controller

import com.drugtracker.model.*
import com.drugtracker.service.CompoundService
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*

/**
 * Controller responsible for handling all HTTP requests related to Compounds.
 *
 * This class is part of the MVC architecture:
 * - Controller: Handles web requests
 * - Service: Contains business logic
 * - Model: Transfers data to the view layer
 * - View: Thymeleaf templates render HTML pages
 *
 * Base URL mapping: /compounds
 *
 * @property service Service layer responsible for compound business operations
 */
@Controller
@RequestMapping("/compounds")
class CompoundController(
    private val service: CompoundService
) {

    /**
     * Displays a list of all compounds.
     *
     * URL: GET /compounds
     *
     * Retrieves all compounds from the service layer and adds them
     * to the model so they can be displayed in the compounds view.
     *
     * @param model Spring UI model used to pass data to the view
     * @return Logical view name for the compounds list page
     */
    @GetMapping
    fun list(model: Model): String {

        // Fetch all compounds from the service layer
        model.addAttribute("compounds", service.getAll())

        // Return the Thymeleaf template name (without .html extension)
        return "compounds"
    }

    /**
     * Displays the form used to create a new compound.
     *
     * URL: GET /compounds/new
     *
     * Prepares:
     * - An empty CompoundRequest object for form binding
     * - Enum values for dropdown selections (status and therapeutic area)
     *
     * @param model Spring UI model used to pass attributes to the view
     * @return Logical view name for the compound creation form
     */
    @GetMapping("/new")
    fun showCreateForm(model: Model): String {

        // Provide an empty request object for form binding
        model.addAttribute("compoundRequest", CompoundRequest(
            name = "",
            molecularFormula = "",
            therapeuticArea = TherapeuticArea.ONCOLOGY
        ))

        // Provide enum values for dropdown menus
        model.addAttribute("statuses", CompoundStatus.values())
        model.addAttribute("areas", TherapeuticArea.values())

        return "compound-form"
    }

    /**
     * Handles submission of the compound creation form.
     *
     * URL: POST /compounds/save
     *
     * Uses:
     * - @Valid to trigger Jakarta Bean Validation
     * - BindingResult to capture validation errors
     *
     * If validation fails:
     * - Re-populates dropdown values
     * - Returns the user to the form page
     *
     * If successful:
     * - Calls service layer to persist the compound
     * - Redirects to compound list page (Post/Redirect/Get pattern)
     *
     * @param compoundRequest Data Transfer Object containing form data
     * @param bindingResult Holds validation errors
     * @param model Spring UI model
     * @return Redirect to list page or return form if validation fails
     */
    @PostMapping("/save")
    fun save(
        @Valid @ModelAttribute compoundRequest: CompoundRequest,
        bindingResult: BindingResult,
        model: Model
    ): String {

        // Check for validation errors triggered by @Valid
        if (bindingResult.hasErrors()) {

            // Re-populate dropdown data (required when returning to the form)
            model.addAttribute("statuses", CompoundStatus.values())
            model.addAttribute("areas", TherapeuticArea.values())

            return "compound-form"
        }

        // Delegate creation logic to service layer
        service.create(compoundRequest)

        // Redirect prevents duplicate form submission (PRG pattern)
        return "redirect:/compounds"
    }

    /**
     * Deletes a compound by its database ID.
     *
     * URL: GET /compounds/delete/{id}
     *
     * Delegates deletion logic to the service layer and
     * redirects back to the compounds list page.
     *
     * @param id Database ID of the compound to delete
     * @return Redirect to compounds list page
     */
    @GetMapping("/delete/{id}")
    fun delete(@PathVariable id: Long): String {

        // Call service layer to remove the compound
        service.delete(id)

        // Redirect back to the list view
        return "redirect:/compounds"
    }
}