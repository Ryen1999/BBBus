package com.bbbus.contentservice.dao.content;

import com.bbbus.contentservice.domain.entity.content.ShareDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ShareDao {


    ShareDTO selectByPrimaryKey(String user_id);

    int updateByPrimaryKeySelective(ShareDTO record);

    /**
     * 分页查询分享列表
     * @param title 标题（模糊查询）
     * @return 分享列表
     */
    List<ShareDTO> selectShareList(@Param("title") String title);

    /**
     * 导出场景分批查询分享列表
     */
    List<ShareDTO> selectShareListForExport(@Param("title") String title,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);

    /**
     * 导出总数统计
     */
    Integer countShareListForExport(@Param("title") String title);

    /**
     * 游标分页导出，避免大offset导致的慢查询
     */
    List<ShareDTO> selectShareListForExportByCursor(@Param("title") String title,
                                                    @Param("lastCreateTime") Date lastCreateTime,
                                                    @Param("lastId") String lastId,
                                                    @Param("limit") Integer limit);

    /**
     * 批量插入（用于 Excel 导入），一条 SQL 插入多行，性能最优
     */
    int batchInsert(@Param("list") List<ShareDTO> list);

}
