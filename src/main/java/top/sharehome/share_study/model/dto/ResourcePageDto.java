package top.sharehome.share_study.model.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教学资料分页回显对象
 *
 * @author AntonyCheng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "教学资料分页回显对象")
public class ResourcePageDto implements Serializable {

    private static final long serialVersionUID = 7489305803184628249L;

    /**
     * 教学资料唯一ID
     */
    private Long id;

    /**
     * 所属老师ID
     */
    private Long belong;

    /**
     * 所属老师姓名
     */
    private String teacherName;

    /**
     * 教学资料名
     */
    private String name;

    /**
     * 教学资料简介
     */
    private String info;

    /**
     * 教学资料所在地址
     */
    private String url;

    /**
     * 教学资料状态（0表示正常，1表示封禁）
     */
    private Integer status;

    /**
     * 教学资料发布时间
     */
    private LocalDateTime createTime;
}
