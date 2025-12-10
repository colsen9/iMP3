package com.imp3.Backend.tag;

import com.imp3.Backend.common.AbstractController;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/tag")
public class TagController extends AbstractController {

    @Autowired
    TagRepository tagrepository;

    @Autowired
    UserTagRepository usertagrepository;

    /**
     * LIST (GET) - Browse/search all tags in the system
     * @param search optional search term
     * @param category optional category filter
     * @return list of tags
     */
    @GetMapping
    public List<Tag> getAllTags(@RequestParam(required = false) String search,
                                @RequestParam(required = false) Tag.TagCategory category){
        if(search != null && !search.isBlank()){
            return tagrepository.findByNameContainingIgnoreCase(search.trim());
        }

        if(category != null) {
            return tagrepository.findByCategory(category);
        }

        return tagrepository.findAll();
    }

    /**
     * READ (GET) - Fetch one specific tag by id
     * @param tagId of the tag
     * @return the tag
     */
    @GetMapping("/{tagId}")
    public Tag getTag(@PathVariable Integer tagId){
        return tagrepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }


    /**
     * CREATE (POST) - Create a new tag
     * @param request containing tag details
     * @param session containing "uid"
     * @return the created tag
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tag createTag(@RequestBody TagRequest request, HttpSession session){
        Integer uid = getSessionUid(session);

        //validate required fields
        if(request.getName() == null || request.getName().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag name is required");
        }

        //check for duplicate tags
        String normalizedName = request.getName().trim().toLowerCase();
        if(tagrepository.existsByNameIgnoreCase(normalizedName)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag aleady exists");
        }

        Tag tag = new Tag();
        tag.setName(normalizedName);

        if(request.getCategory() != null){
            tag.setCategory(request.getCategory());
        }

        if(request.getDescription() != null){
            tag.setDescription(request.getDescription());
        }

        return tagrepository.save(tag);
    }

    /**
     *  UPDATE (PUT) - Rename or update a tag
     * @param tagId of the tag
     * @param request containing updated details
     * @param session containing "uid"
     * @return the updated tag
     */
    @PutMapping("/{tagId}")
    public Tag updateTag(@PathVariable Integer tagId,
                         @RequestBody TagRequest request, HttpSession session){
        Integer uid = getSessionUid(session);

        Tag tag = tagrepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));

        if(request.getName() != null && !request.getName().isBlank()){
            String normalizedName = request.getName().trim().toLowerCase();

            tagrepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
                if(!existing.getTagId().equals(tagId)){
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag name already exists");
                }
            });
        tag.setName(normalizedName);
        }

        if(request.getCategory() != null){
            tag.setCategory(request.getCategory());
        }

        if(request.getDescription() != null){
            tag.setDescription(request.getDescription());
        }

        return tagrepository.save(tag);
    }

    /**
     * DELETE (DELETE) - Delete a tag from the system
     * @param tagId of the tag
     * @param session containing "uid"
     */
    @DeleteMapping("/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable Integer tagId, HttpSession session){

        Tag tag = tagrepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));

        // only admins can delete tags
        String userType = (String)session.getAttribute("type");
        if (!userType.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Must be Admin");
        }

        //delete all userTag references first
        usertagrepository.deleteByTag_TagId(tagId);

        //delete the tag
        tagrepository.delete(tag);
    }


}
