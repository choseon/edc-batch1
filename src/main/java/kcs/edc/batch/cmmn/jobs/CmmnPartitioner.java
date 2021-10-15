package kcs.edc.batch.cmmn.jobs;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Common Partitioner
 * list와 gridSize로 분할하여 Thread로 실행한다.
 */
@Slf4j
@StepScope
public class CmmnPartitioner implements Partitioner {

    @Value("#{jobExecutionContext[list]}")
    protected List<Object> list;

    @SneakyThrows
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        int size = list.size();
        log.info("Partitioner Total Count >> {}", size);

        int range = (size/gridSize) + 1;
        int from = 0;
        int to = range;

        Map<String, ExecutionContext> result = new HashMap<String, ExecutionContext>();

        for (int i = 1; i <= gridSize; i++) {
            ExecutionContext value = new ExecutionContext();

            List<Object> partitionList = new ArrayList<>();
            for (int j = from; j < to; j++) {
                partitionList.add(list.get(j));
            }
            value.putInt("threadNum", i);
            value.put("partitionList", partitionList);

            result.put("partition" + i, value);

            from = to + 1;
            to += range;
            if (to >= size) to = size;

            log.info("Partitioner Starting : Thread {}, size {}, from {}, to {}", i, partitionList.size(), from, to);
        }

        return result;
    }
}
