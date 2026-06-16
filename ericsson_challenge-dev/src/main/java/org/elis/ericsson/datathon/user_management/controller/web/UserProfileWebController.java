package org.elis.ericsson.datathon.user_management.controller.web;

import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserPrincipal;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.exception.ItemNotFoundException;
import org.elis.ericsson.datathon.user_management.repository.RoleRepository;
import org.elis.ericsson.datathon.user_management.service.UserProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.elis.ericsson.datathon.user_management.constants.ExceptionMessages.ROLE_NOT_FOUND;

@Controller
@RequestMapping("/profiles")
public class UserProfileWebController {

    private final UserProfileService userProfileService;
    private final RoleRepository roleRepository;

    public UserProfileWebController(UserProfileService userProfileService, RoleRepository roleRepository) {
        this.userProfileService = userProfileService;
        this.roleRepository = roleRepository;
    }

    @ModelAttribute("roles")
    public List<Role> roles() {
        return roleRepository.findAll();
    }

    @GetMapping
    public String getAllProfiles(Model model, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<UserProfile> profiles = userProfileService.getAllProfiles();
        model.addAttribute("profiles", profiles);
        model.addAttribute("loggedInUserId", userPrincipal.getId());
        return "userprofile/profiles";
    }

    @GetMapping("/add-profile")
    public String showAddProfileForm(Model model) {
        model.addAttribute("newProfile", new UserProfile());
        return "userprofile/addProfile";
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addProfile(@ModelAttribute("newProfile") UserProfile newProfile, @RequestParam("roles") List<Long> roleIds) {
        List<Role> roles = new ArrayList<>();
        for (Long roleId : roleIds) {
            roles.add(roleRepository.findById(roleId)
                    .orElseThrow(() -> new ItemNotFoundException(ROLE_NOT_FOUND)));
        }
        newProfile.setRoles(roles);
        userProfileService.addProfile(newProfile);
        return "redirect:/profiles";
    }

    @GetMapping("/edit/{id}")
    public String showEditProfileForm(@PathVariable Long id, Model model) {
        UserProfile userProfile = userProfileService.getProfileById(id);
        model.addAttribute("editProfile", userProfile);
        return "userprofile/editProfile";
    }

    @PostMapping("/edit/{id}")
    public String editProfile(@PathVariable Long id, @ModelAttribute("editProfile") UserProfile updatedProfile,
                              @RequestParam("roles") List<Long> roleIds,
                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Non-admin users can only edit their own profile and cannot change roles
        if (!isAdmin) {
            if (!userPrincipal.getId().equals(id)) {
                throw new org.springframework.security.access.AccessDeniedException("Cannot edit another user's profile");
            }
            // Preserve existing roles — non-admin cannot modify roles
            UserProfile existing = userProfileService.getProfileById(id);
            updatedProfile.setRoles(existing.getRoles());
        } else {
            List<Role> roles = new ArrayList<>();
            for (Long roleId : roleIds) {
                roles.add(roleRepository.findById(roleId)
                        .orElseThrow(() -> new ItemNotFoundException(ROLE_NOT_FOUND)));
            }
            updatedProfile.setRoles(roles);
        }

        userProfileService.editProfile(id, updatedProfile);
        return "redirect:/profiles";
    }
}
