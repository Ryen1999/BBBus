package com.bbbus.contentservice.dao.content;

import com.bbbus.contentservice.domain.entity.content.Share;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface ShareDao {


    Share selectByPrimaryKey(String user_id);

    int updateByPrimaryKeySelective(Share record);

}
