package gr.uoa.di.api;

import gr.uoa.di.dao.CategoryEntity;
import gr.uoa.di.dao.ItemEntity;
import gr.uoa.di.dto.category.CategoryResponseDto;
import gr.uoa.di.dto.category.CategorySimpleResponseDto;
import gr.uoa.di.dto.item.ItemResponseDto;
import gr.uoa.di.exception.category.CategoryNotFoundException;
import gr.uoa.di.mapper.CategoryMapper;
import gr.uoa.di.mapper.ItemMapper;
import gr.uoa.di.repo.CategoryRepository;
import gr.uoa.di.repo.ItemRepository;
import gr.uoa.di.service.ItemService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryApi {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    CategoryMapper categoryMapper;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    ItemMapper itemMapper;

    @Autowired
    ItemService itemService;

    private CategoryResponseDto getCategoriesRecursive(CategoryEntity catEnt) {
        CategoryResponseDto cat = categoryMapper.mapCategoryEntityToCategoryResponseDto(catEnt, false, 0);
        if (catEnt.getSubcategories() != null) {
            cat.setSubcategories(catEnt.getSubcategories().stream().map(this::getCategoriesRecursive).collect(Collectors.toList()));
        }
        return cat;
    }

    @Cacheable("top_categories")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public List<CategoryResponseDto> getCategoriesAndSubsInDepth1() {
        /* get top categories and their subcategories */
        return categoryRepository.findByParentCategoryIsNull().stream().map(cat
                -> categoryMapper.mapCategoryEntityToCategoryResponseDto(cat, false, 1)).collect(Collectors.toList());
    }

    @Cacheable("all_categories")
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public List<CategorySimpleResponseDto> getAllCategories() {
        Deque<Pair<Integer, CategoryEntity>> catStack = new ArrayDeque<>(
                categoryRepository.findByParentCategoryIsNull().stream().map(
                        categoryEntity -> new ImmutablePair<>(0, categoryEntity)
                ).collect(Collectors.toList()));
        List<CategorySimpleResponseDto> respList = new LinkedList<>();

        while (!catStack.isEmpty()) {
            Pair<Integer, CategoryEntity> pair = catStack.pop();
            Integer prefixLen = pair.getLeft();
            String prefix = StringUtils.repeat('-', prefixLen) + " ";
            CategoryEntity cat = pair.getRight();
            respList.add(new CategorySimpleResponseDto(cat.getId(), prefix + cat.getName()));
            cat.getSubcategories().forEach(subcat ->
                catStack.push(new ImmutablePair<>(prefixLen + 1, subcat))
            );
        }

        return respList;
    }

    @RequestMapping(value = "/{categoryId}")
    public CategoryResponseDto getCategoryAndSubsInDepth2(@PathVariable Integer categoryId) {
        CategoryEntity category = categoryRepository.findOneById(categoryId);
        if (category == null) {
            throw new CategoryNotFoundException();
        }
        return categoryMapper.mapCategoryEntityToCategoryResponseDto(category, true, 2);
    }

    @RequestMapping(value = "/{categoryId}/items")
    public Page<ItemResponseDto> getCategoryItems(@PathVariable Integer categoryId, Pageable pageable) {
        if (categoryRepository.findOneById(categoryId) == null) {
            throw new CategoryNotFoundException();
        }

        Page<ItemEntity> page;
        do {
            /* finalize items that need to be finalized before returning the page if necessary */
            page = itemRepository.findByCategory_IdOrderByFinishedAscStartDateDesc(categoryId, pageable);
        } while (!itemService.finalizeFinishedPageItems(page));
        return page.map(itemMapper::mapItemEntityToItemResponseDto);
    }
}
