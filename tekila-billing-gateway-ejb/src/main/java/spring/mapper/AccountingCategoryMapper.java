package spring.mapper;

import com.jaravir.tekila.module.accounting.entity.AccountingCategory;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;
import spring.dto.AccountingCategoryDTO;

@Mapper(componentModel = "spring")
public interface AccountingCategoryMapper extends EntityMapper<AccountingCategoryDTO,AccountingCategory>{

}
