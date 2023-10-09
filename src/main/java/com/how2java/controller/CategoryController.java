package com.how2java.controller;

import com.how2java.pojo.Category;
import com.how2java.service.CategoryService;
import com.how2java.util.ImageUtil;
import com.how2java.util.Page4Navigator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
public class CategoryController {
    @Autowired
    CategoryService categoryService;

    @GetMapping("/categories")
    public Page4Navigator<Category> list(@RequestParam(value = "start", defaultValue = "0") int start, @RequestParam(value = "size", defaultValue = "5") int size) throws Exception {
        start = start<0?0:start;
        Page4Navigator<Category> page =categoryService.list(start, size, 5);  //5表示导航分页最多有5个，像 [1,2,3,4,5] 这样
        return page;
    }

    /*
    1. 首选通过CategoryService 保存到数据库
    2. 然后接受上传图片，并保存到 img/category目录下
    3. 文件名使用新增分类的id
    4. 如果目录不存在，需要创建
    5. image.transferTo 进行文件复制
    6. 调用ImageUtil的change2jpg 进行文件类型强制转换为 jpg格式
    7. 保存图片
    * */

    @PostMapping("/categories")
    public Object add(Category bean, MultipartFile image, HttpServletRequest request) throws Exception {
        categoryService.add(bean);
        saveOrUpdateImageFile(bean, image, request);
        return bean;
    }
    // MultipartFile image对象通常代表了一个用户通过表单上传的文件，比如图片文件
    // file对象代表了服务器上将要保存这个上传文件的目标位置。
    public void saveOrUpdateImageFile(Category bean, MultipartFile image, HttpServletRequest request) throws IOException{
        File imageFolder = new File(request.getServletContext().getRealPath("img/category"));
        File file = new File(imageFolder,bean.getId()+".jpg");
        if(!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        image.transferTo(file);
        BufferedImage img = ImageUtil.change2jpg(file);
        ImageIO.write(img,"jpg",file);
    }

    /*
    1. 首先根据 id 删除数据库里的数据
    2. 删除对应的文件
    3. 返回 null, 会被RESTController 转换为空字符串。
    * */
    @DeleteMapping("/categories/{id}")
    public String delete(@PathVariable("id") int id, HttpServletRequest request) throws Exception{
        categoryService.delete(id);
        File imageFolder = new File(request.getServletContext().getRealPath("img/category"));
        File file = new File(imageFolder,id+".jpg");
        file.delete();
        return null;
    }

    // 提供 get 方法，把 id 对应的 Category 取出来，并转换为json对象发给浏览器
    @GetMapping("/categories/{id}")
    public Category get(@PathVariable("id") int id) throws Exception {
        Category bean=categoryService.get(id);
        return bean;
    }

    /*
    1. 获取参数名称,这里获取参数用的是 request.getParameter("name"). 为什么不用 add里的注入一个 Category对象呢？
     因为。。。PUT 方式注入不了。。。 只能用这种方式取参数了，试了很多次才知道是这么个情况~
    2. 通过 CategoryService 的update方法更新到数据库
    3. 如果上传了图片，调用增加的时候共用的 saveOrUpdateImageFile 方法。
    4. 返回这个分类对象。
    * */
    @PutMapping("/categories/{id}")
    public Object update(Category bean, MultipartFile image,HttpServletRequest request) throws Exception {
        String name = request.getParameter("name");
        bean.setName(name);
        categoryService.update(bean);

        if(image!=null) {
            saveOrUpdateImageFile(bean, image, request);
        }
        return bean;
    }
}
