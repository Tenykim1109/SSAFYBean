package com.ssafy.smartstore.model.service;

import com.ssafy.smartstore.model.dto.UserCoupon;

import java.util.List;
import java.util.Map;

public interface UserCouponService {
    List<Map<String, Object>> getCouponByUserId(String userId);

    List<Map<String, Object>> getUsedCouponByUserId(String userId);

    List<String> getExpiredCouponId();

    Map<String, Object> selectCoupon(Integer id);

    int usingCoupon(Integer id);

    int setExpired(String id);

    int insertCoupon(UserCoupon coupon);
}
