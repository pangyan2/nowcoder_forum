package com.nowcoder.community.controller;

import com.nowcoder.community.service.DataService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

/**
 * @author Alex
 * @version 1.0
 * @company xxx
 * @copyright (c)  xxxInc. All rights reserved.
 * @date 2022/2/21 12:24
 */
@Controller
@RequestMapping("/data")
@Api(tags = "数据管理接口")
public class DataController {

    @Autowired
    private DataService dataService;

    /**
     * 数据统计页面
     * @return
     */
    @RequestMapping(path = "/dataViewPage",method = {RequestMethod.GET,RequestMethod.POST})
    public String getDataPage(){
        return "/site/admin/data";
    }

    /**
     * 统计网站uv
     * @param start
     * @param end
     * @param model
     * @return
     */
    @RequestMapping(path = "/uv",method = RequestMethod.POST)
    public String getUv(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end,
                        Model model){
        long uv = dataService.calculateUv(start,end);
        model.addAttribute("uvResult",uv);
        model.addAttribute("uvStartDate",start);
        model.addAttribute("uvEndDate",end);
        return "forward:/data/dataViewPage";
    }

    /**
     * 统计活跃用户
     * @param start
     * @param end
     * @param model
     * @return
     */
    @RequestMapping(path = "/dau",method = RequestMethod.POST)
    public String getUau(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd")Date end,
                        Model model){
        long dau = dataService.calculateDau(start,end);
        model.addAttribute("dauResult",dau);
        model.addAttribute("dauStartDate",start);
        model.addAttribute("dauEndDate",end);
        return "forward:/data/dataViewPage";
    }


}
