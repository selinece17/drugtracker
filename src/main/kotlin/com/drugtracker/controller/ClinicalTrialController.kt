package com.drugtracker.controller

import com.drugtracker.model.*
import com.drugtracker.service.CompoundService
import com.drugtracker.service.ClinicalTrialService
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
/**
 * Controller responsible for handling all HTTP requests related to Clinical Trials.
 *
 * This controller follows the MVC (Model-View-Controller) architecture:
 *
 * - Controller: Handles incoming HTTP requests
 * - Service Layer: Contains business logic
 * - Model: Transfers data between controller and view
 * - View (Thymeleaf templates): Renders HTML pages
 *
 * Base URL mapping: /trials
 *
 * Dependencies:
 * @property service Handles clinical trial business logic
 * @property compoundService Provides compound data for trial association
 */
@Controller
@RequestMapping("/trials")
class ClinicalTrialController(
    private val service: ClinicalTrialService,
    private val compoundService: CompoundService
) {
    /**
     * Displays the list of all clinical trials.
     *
     * URL: GET /trials
     *
     * @param model Spring UI model used to pass attributes to the view
     * @return Name of the Thymeleaf template that renders the trial list page
     */
    @GetMapping
    fun list(model: Model): String {
        // Fetch all trials from the service layer
        model.addAttribute("trials", service.getAll())
        // Return the HTML page that displays the trials
        return "trials"
    }

    /**
     * Displays the form used to create a new clinical trial.
     *
     * URL: GET /trials/new
     *
     * Prepares:
     * - An empty ClinicalTrialRequest object for form binding
     * - Enum values for dropdown selections (phase, status)
     * - List of compounds to associate with the trial
     *
     * @param model Spring UI model used to send data to the view
     * @return Name of the Thymeleaf form template
     */
    @GetMapping("/new")
    fun showForm(model: Model): String {
        // Create an empty request object for form binding
        model.addAttribute("trialRequest", ClinicalTrialRequest(
            compoundId = null,
            trialId = "",
            title = "",
            phase = TrialPhase.PHASE_1
        ))
        // Provide enum values for dropdown menus
        model.addAttribute("phases", TrialPhase.values())
        model.addAttribute("statuses", TrialStatus.values())
        // Provide available compounds for selection
        model.addAttribute("compounds", compoundService.getAll())
        return "trial-form"
    }

    /**
     * Handles form submission for creating a new clinical trial.
     *
     * URL: POST /trials/save
     *
     * Validation:
     * - Uses @Valid to trigger Jakarta Bean Validation
     * - BindingResult captures validation errors
     *
     * If validation fails:
     * - Re-populates dropdown data
     * - Returns user to form with error messages
     *
     * If successful:
     * - Calls service layer to persist data
     * - Redirects to trial list page
     *
     * @param trialRequest DTO containing submitted form data
     * @param bindingResult Contains validation errors (if any)
     * @param model Spring UI model
     * @return Redirect to list page or form view if errors occur
     */
    @PostMapping("/save")
    fun save(
        @Valid @ModelAttribute("trialRequest") trialRequest: ClinicalTrialRequest,
        bindingResult: BindingResult,
        model: Model
    ): String {
        // Check for validation errors from @Valid annotations
        if (bindingResult.hasErrors()) {
            // Re-populate form dropdown data (required when returning to form)
            model.addAttribute("phases", TrialPhase.values())
            model.addAttribute("statuses", TrialStatus.values())
            model.addAttribute("compounds", compoundService.getAll())
            // Re-attach submitted data so the user doesn't lose input
            model.addAttribute("trialRequest", trialRequest) // <-- add this
            return "trial-form"
        }

        try {
            // Delegate creation logic to service layer
            service.create(trialRequest)
            return "redirect:/trials"
        } catch (ex: Exception) {
            // If business logic fails, re-populate dropdown data
            model.addAttribute("phases", TrialPhase.values())
            model.addAttribute("statuses", TrialStatus.values())
            model.addAttribute("compounds", compoundService.getAll())
            model.addAttribute("errorMessage", ex.message)
            return "trial-form"
        }
    }

    /**
     * Deletes a clinical trial by its database ID.
     *
     * URL: GET /trials/delete/{id}
     *
     * @param id Database ID of the trial to delete
     * @return Redirects to the trial list page
     */
    @GetMapping("/delete/{id}")
    fun delete(@PathVariable id: Long): String {
        // Delegate deletion logic to service layer
        service.delete(id)
        // Redirect back to trial list
        return "redirect:/trials"
    }
}