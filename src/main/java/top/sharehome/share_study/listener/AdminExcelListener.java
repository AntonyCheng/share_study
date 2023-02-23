package top.sharehome.share_study.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import top.sharehome.share_study.common.constant.CommonConstant;
import top.sharehome.share_study.mapper.TeacherMapper;
import top.sharehome.share_study.model.entity.Teacher;

import javax.annotation.Resource;
import java.util.List;

/**
 * EasyExcel Admin 监听类
 *
 * @author AntonyCheng
 */
@Component
public class AdminExcelListener extends AnalysisEventListener<Teacher> {

    @Resource
    private TeacherMapper teacherMapper;

    @Override
    public void invoke(Teacher teacher, AnalysisContext analysisContext) {
        teacherMapper.insert(teacher);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }
}
