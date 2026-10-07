package com.mall.iam.infrastructure.persistence.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.iam.infrastructure.persistence.account.po.IamAccountProfilePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IamAccountProfileMapper
        extends BaseMapper<IamAccountProfilePO> {
}
