package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TravelDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @NotBlank(message = "省份不能为空")
    @Size(max = 30, message = "省份不能超过30字")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市不能超过50字")
    private String city;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalDate travelDate;

    @Size(max = 10, message = "季节不能超过10字")
    private String season;

    @Size(max = 20, message = "天气不能超过20字")
    private String weather;

    @Size(max = 2000, message = "手记不能超过2000字")
    private String note;

    @Size(max = 500, message = "封面地址不能超过500字")
    private String cover;

    /** 状态：0-想去 1-已去过 */
    private Integer status;

    private Integer sortOrder;
}
