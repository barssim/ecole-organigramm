package com.example.SkilToDB.service;

import com.example.SkilToDB.model.Skill;
import com.example.SkilToDB.repository.SkillRepository;
import com.example.SkilToDB.utils.ExcelParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.List;

@Service
public class ExcelService {
    @Autowired
    private SkillRepository repo;

    public void saveSkills(MultipartFile file) throws IOException {
        List<Skill> skills = ExcelParser.parseExcel(file.getInputStream());
        repo.saveAll(skills);
    }
}


