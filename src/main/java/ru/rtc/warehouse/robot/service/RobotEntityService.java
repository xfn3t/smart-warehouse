package ru.rtc.warehouse.robot.service;

import java.util.List;
import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.robot.model.Robot;

public interface RobotEntityService extends CrudEntityService<Robot, Long> {
    Robot findByCode(String code);
    Integer findMaxRobotNumber();
    Robot saveAndFlush(Robot robot);

    Integer getTotalRobotsCount(Long id);

    List<Robot> findAllByWarehouseCode(String warehouseCode);

    List<Robot> findAllWithWarehouse();

    void delete(String robotCode);
}
