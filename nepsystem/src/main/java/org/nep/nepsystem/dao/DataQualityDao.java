package org.nep.nepsystem.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.nep.nepsystem.bean.DataQuality;

/** data_quality 表 DAO（业务层升级新增） */
@Mapper
public interface DataQualityDao extends BaseMapper<DataQuality> {
}
