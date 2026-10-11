package com.wcy.shorturl.domain.pro;

import com.wcy.shorturl.domain.po.TUrlMapping;
import lombok.Data;

@Data
public class Url_lock {
    private String code;
    private TUrlMapping tUrlMapping;
    private Boolean iscontinue;
}
