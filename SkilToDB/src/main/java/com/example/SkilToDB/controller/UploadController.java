package com.example.SkilToDB.controller;

@RestController
public class UploadController {
    @Autowired private ExcelService service;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            service.saveSkills(file);
            return ResponseEntity.ok("Datei erfolgreich in DB gespeichert!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Fehler: " + e.getMessage());
        }
    }
}
