package com.zipcode.stardust.config;

import com.zipcode.stardust.model.Subforum;
import com.zipcode.stardust.repository.SubforumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private SubforumRepository subforumRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (subforumRepository.count() == 0) {
            Subforum forum = new Subforum("Forum",
                    "Announcements, bug reports, and general discussion about the forum belongs here", null);
            subforumRepository.save(forum);

            Subforum announcements = new Subforum("Announcements",
                    "View forum announcements here", forum);
            subforumRepository.save(announcements);

            Subforum bugReports = new Subforum("Bug Reports",
                    "Report bugs with the forum here", forum);
            subforumRepository.save(bugReports);

            Subforum general = new Subforum("General Discussion",
                    "Use this subforum to post anything you want", null);
            subforumRepository.save(general);

            Subforum other = new Subforum("Other",
                    "Discuss other things here", null);
            subforumRepository.save(other);
        }
    }
}
