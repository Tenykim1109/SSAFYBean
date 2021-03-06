package com.ssafy.smartstore.model.dao;

import com.ssafy.smartstore.model.dto.Coupon;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface CouponDao {
    int insert(Coupon coupon);

    List<Coupon> select();

    int update(Coupon coupon);
}
