package ru.styk.supply;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.LoggerFactory;

@Component
public class ReplenishmentScheduler {
    private final SupplyService supplies;
    public ReplenishmentScheduler(SupplyService supplies){this.supplies=supplies;}
    @Scheduled(fixedDelayString="${styk.replenishment-delay}",initialDelay=60000)
    public void run(){try{supplies.replenish();}catch(RuntimeException e){LoggerFactory.getLogger(getClass()).error("Replenishment failed; next scheduled execution will retry",e);}}
}
