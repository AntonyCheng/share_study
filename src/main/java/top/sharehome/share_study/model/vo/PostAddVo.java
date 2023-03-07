package top.sharehome.share_study.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.sharehome.share_study.common.converter.ExcelLongConverter;

import java.io.Serializable;

/**
 * 添加帖子Vo
 *
 * @author AntonyCheng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("添加帖子Vo")
public class PostAddVo implements Serializable {

    private static final long serialVersionUID = -5409731863134275220L;

    /**
     * 教学资料所有人唯一ID
     */
    private Long belong;

    /**
     * 教学资料名称
     */
    private String name;

    /**
     * 教学资料简介
     */
    private String info;

    /**
     * 教学资料OSS链接
     */
    private String url;
}
