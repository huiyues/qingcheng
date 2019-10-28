package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Spu;
import com.qingcheng.service.goods.SpuService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/spu")
public class SpuController {

    @Reference
    private SpuService spuService;

    @GetMapping("/findAll")
    public List<Spu> findAll() {
        return spuService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Spu> findPage(int page, int size) {
        return spuService.findPage(page, size);
    }

    @PostMapping("/fin dList")
    public List<Spu> findList(@RequestBody Map<String, Object> searchMap) {
        return spuService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Spu> findPage(@RequestBody Map<String, Object> searchMap, int page, int size) {
        return spuService.findPage(searchMap, page, size);
    }

    @GetMapping("/findById")
    public Spu findById(String id) {
        return spuService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Spu spu) {
        spuService.add(spu);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Spu spu) {
        spuService.update(spu);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(String id) {
        spuService.delete(id);
        return new Result();
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: spu详细信息保存
     */
    @PostMapping("/save")
    public Result save(@RequestBody Goods goods) {
        spuService.saveSpu(goods);

        return new Result();
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 根据id查询spu详细信息
     */
    @GetMapping("/findGoodById")
    public Goods findGoodsById(String id) {
        Goods goods = spuService.findGoodsById(id);

        return goods;
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 商品审核操作
     */
    @PostMapping("/audit")
    public Result audit(@RequestBody Map<String, String> map) {
        spuService.audit(map.get("id"), map.get("status"), map.get("message"));

        return new Result();
    }


    //修改下架
    @GetMapping("/pull")
    public Result pull(String id) {
        spuService.pull(id);

        return new Result();
    }

    //修改上架
    @GetMapping("/put")
    public Result put(String id) {
        spuService.put(id);

        return new Result();
    }

    //修改上架
    @GetMapping("/putMany")
    public Result putMany(String[] ids) {
        int count = spuService.putMany(ids);

        return new Result(0, "上架了" + count + "个商品");
    }

    //删除操作（逻辑删除以及删除还原）
    @GetMapping("/isDelete")
    public Result del(String id) {
        spuService.del(id);

        return new Result();
    }


    //物理删除真正的删除
    @GetMapping("/deleteId")
    public Result deleteId(String id) {
        spuService.deleteId(id);

        return new Result();
    }
}
