package ru.rtc.warehouse.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.dashboard.events.RobotSnapshotEvent;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.repository.RobotRepository;


@Service
@RequiredArgsConstructor
public class RobotCommandService {
    private final RobotRepository repo;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public Robot update(Robot r) {
        Robot saved = repo.save(r);
        publisher.publishEvent(new RobotSnapshotEvent(this, saved));
        return saved;
    }
}