package com.devsecops.project.controller;

import com.devsecops.project.service.FileStorageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Upload demonstration page (see FileStorageService for VULNERABILITY #21). */
@Controller
public class FileUploadController {

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/upload")
    public String page(Model model) {
        model.addAttribute("files", fileStorageService.list());
        return "upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        String saved = fileStorageService.save(file);
        if (saved == null) {
            redirectAttributes.addFlashAttribute("error", "The file could not be saved.");
        } else {
            redirectAttributes.addFlashAttribute("message", "File uploaded: " + saved);
        }
        return "redirect:/upload";
    }
}
