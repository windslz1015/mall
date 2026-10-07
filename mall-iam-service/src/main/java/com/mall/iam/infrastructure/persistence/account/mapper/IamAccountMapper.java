package com.mall.iam.infrastructure.persistence.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.iam.infrastructure.persistence.account.po.IamAccountPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IamAccountMapper
        extends BaseMapper<IamAccountPO> {
}
