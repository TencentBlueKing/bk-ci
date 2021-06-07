package com.tencent.bk.codecc.defect.model.checkerset;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "t_checker_set_snapshot")
@CompoundIndexes({
    @CompoundIndex(name = "checker_set_id_1_create_date_1", def = "{'checker_set_id': 1, 'create_date': 1}", background = true)
})
public class CheckerSetSnapShotEntity extends CheckerSetEntity {
}
