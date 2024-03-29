package com.learningplatform.app.smart_learn.managerController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.learningplatform.app.smart_learn.domain.Course;
import com.learningplatform.app.smart_learn.domain.LearningContent;
import com.learningplatform.app.smart_learn.domain.Message;
import com.learningplatform.app.smart_learn.domain.User;
import com.learningplatform.app.smart_learn.model.CourseDTO;
import com.learningplatform.app.smart_learn.model.LearningContentDTO;
import com.learningplatform.app.smart_learn.model.MessageDto;
import com.learningplatform.app.smart_learn.repos.CourseRepository;
import com.learningplatform.app.smart_learn.repos.LearningContentRepository;
import com.learningplatform.app.smart_learn.repos.MessageRepo;
import com.learningplatform.app.smart_learn.repos.UserRepository;
import com.learningplatform.app.smart_learn.service.CourseService;
import com.learningplatform.app.smart_learn.service.LearningContentService;
import com.learningplatform.app.smart_learn.util.WebUtils;

import jakarta.validation.Valid;

@SuppressWarnings("null")
@Controller
@RequestMapping("/manager")
public class managerHomeController {
    @Autowired
    LearningContentRepository learningContentRepository;

    @Autowired
    LearningContentService learningContentService;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String getManager() {
        return "managerHome/manager";
    }

    @GetMapping("/profile")
    public String getUserprofile(Model m) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username).get();
        m.addAttribute("obj", user);
        return "managerHome/managerProfile";
    }

    @GetMapping("/courses")
    public String list(final Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username).get();

        List<Course> courses = courseRepository.findByTutor(user);

        model.addAttribute("courses", courses);
        return "managerHome/list";
    }

    @GetMapping("/courses/add")
    public String add(@ModelAttribute("course") final CourseDTO courseDTO) {
        return "managerHome/add";
    }

    @PostMapping("/courses/add")
    public String add(@ModelAttribute("course") @Valid final CourseDTO courseDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "managerHome/add";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username).get();
        Course course = new Course();
        course.setCourseTitle(courseDTO.getCourseTitle());
        course.setCourseDuration(courseDTO.getCourseDuration());
        course.setCoursePrice(courseDTO.getCoursePrice());
        course.setCourseDescription(courseDTO.getCourseDescription());
        course.setCourseType(courseDTO.getCourseType());
        course.setTutor(user);
        courseRepository.save(course);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("course.create.success"));
        return "redirect:/manager/courses";
    }

    @GetMapping("/courses/edit/{courseId}")
    public String edit(@PathVariable(name = "courseId") final Integer courseId, final Model model) {
        model.addAttribute("course", courseService.get(courseId));
        return "managerHome/edit";
    }

    @PostMapping("/courses/edit/{courseId}")
    public String edit(@PathVariable(name = "courseId") final Integer courseId,
            @ModelAttribute("course") @Valid final CourseDTO courseDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "managerHome/edit";
        }
        courseService.update(courseId, courseDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("course.update.success"));
        return "redirect:/manager/courses";
    }
    // todo delete the course
    // @PostMapping("/courses/delete/{courseId}")
    // public String delete(@PathVariable(name = "courseId") final Integer courseId,
    // final RedirectAttributes redirectAttributes) {
    // final ReferencedWarning referencedWarning =
    // courseService.getReferencedWarning(courseId);
    // if (referencedWarning != null) {
    // redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR,
    // WebUtils.getMessage(referencedWarning.getKey(),
    // referencedWarning.getParams().toArray()));
    // } else {
    // courseService.delete(courseId);
    // redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO,
    // WebUtils.getMessage("course.delete.success"));
    // }
    // return "redirect:/manager/courses";
    // }

    @GetMapping("/learningContent/{courseId}")
    public String getmanageCourseDataget(@PathVariable(name = "courseId") final Integer courseId, final Model model,
            final RedirectAttributes redirectAttributes, @RequestParam(defaultValue = "0") int page) {

        int PAGE_SIZE = 3;

        Course course = courseRepository.findById(courseId).orElse(null);

        if (course == null) {
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR, WebUtils.getMessage("Error, something is wrong."));
            return "redirect:/manager"; // You can replace "errorPage" with the actual error page name
        }

        model.addAttribute("courseObj", course);

        Page<LearningContent> x = learningContentRepository.findByCourse(course, PageRequest.of(page, PAGE_SIZE));

        model.addAttribute("learningContentList", x);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", x.getTotalPages());

        int nextPage = page + 1;
        int previousPage = page - 1;
        int firstPage = 0;
        int lastPage = x.getTotalPages() - 1;

        model.addAttribute("firstPageUrl", "/manager/learningContent/" + courseId + "?page=" + firstPage);
        model.addAttribute("previousPageUrl", "/manager/learningContent/" + courseId + "?page=" + previousPage);
        model.addAttribute("nextPageUrl", "/manager/learningContent/" + courseId + "?page=" + nextPage);
        model.addAttribute("lastPageUrl", "/manager/learningContent/" + courseId + "?page=" + lastPage);

        int startPage = Math.max(0, page - 2);
        int endPage = Math.min(lastPage, page + 4);

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "managerHome/learningContent";
    }

    @GetMapping("/learningContentAdd/{courseId}")
    public String getmanageCourseAdd(@PathVariable(name = "courseId") final Integer courseId, final Model model) {
        model.addAttribute("courseObj", courseRepository.findById(courseId).get());
        LearningContentDTO contentDTO = new LearningContentDTO();
        contentDTO.setCourse(courseId);
        model.addAttribute("obj", contentDTO);
        return "managerHome/learningContentAdd";
    }

    @PostMapping("/learningContent")
    public String learningContentpost(@ModelAttribute("obj") LearningContentDTO learningContentDTO,
            final RedirectAttributes redirectAttributes, @RequestParam("file") MultipartFile file) throws IOException {
        try {
            LearningContent learningContent = new LearningContent();

            learningContent = learningContentService.mapToEntity(learningContentDTO, learningContent);
            learningContent.setUnit(learningContentDTO.getUnit());
            if (!file.isEmpty()) {
                learningContent.setPostImage(file.getBytes());
            }
            learningContentRepository.save(learningContent);

        } catch (Exception e) {
        }
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS,
                WebUtils.getMessage("Learning Content Added successfully"));
        return "redirect:/manager/learningContent/" + learningContentDTO.getCourse();
    }

    @GetMapping("/learningContent/delete/{learningContentId}")
    public String LearningContentDelete(@PathVariable(name = "learningContentId") final Integer learningContentId,
            final RedirectAttributes redirectAttributes, final Model model) {

        int a = learningContentRepository.findById(learningContentId).get().getCourse().getCourseId();
        learningContentRepository.deleteById(learningContentId);

        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS,
                WebUtils.getMessage("Learning Content Delete successfully"));

        return "redirect:/manager/learningContent/" + a;
    }

    @GetMapping("/learningContent/image/{id}")
    public ResponseEntity<byte[]> getPost(@PathVariable("id") int id, Model model) {
        LearningContent post = learningContentRepository.findById(id).get();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(post.getPostImage());
    }

    @GetMapping("/learningContent/edit/{learningContentId}")
    public String LearningContentEdit(@PathVariable(name = "learningContentId") final Integer learningContentId,
            final Model model) {

        model.addAttribute("learningContent", learningContentService
                .mapToDTO(learningContentRepository.findById(learningContentId).get(), new LearningContentDTO()));
        return "managerHome/learningContentEdit";
    }

    @PostMapping("/learningContent/edit/{learningContentId}")
    public String LearningContentEditPost(@PathVariable(name = "learningContentId") final Integer learningContentId,
            @ModelAttribute("learningContent") @Valid final LearningContentDTO learningContentDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (bindingResult.hasErrors()) {
            return "managerHome/learningContentEdit";
        }
        LearningContent learningContent = new LearningContent();
        learningContent = learningContentService.mapToEntity(learningContentDTO, new LearningContent());
        learningContent.setContentId(learningContentId);

        if (file != null && !file.isEmpty()) {
            learningContent.setPostImage(file.getBytes());
        } else {
            learningContent.setPostImage(learningContentRepository.findById(learningContentId).get().getPostImage());
        }
        learningContentRepository.save(learningContent);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS,
                WebUtils.getMessage("Learning Content update successfully"));
        return "redirect:/manager/learningContent/" + learningContentDTO.getCourse();
    }

    @GetMapping("/videoCall")
    public String VideoCall(Model model) {
        return "managerHome/videoCall";
    }

    @Autowired
    MessageRepo messageRepo;

    @GetMapping("/chat")
    public String getUserChatPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username).orElse(null);
        List<User> students = courseRepository.findByTutor(user)
                .stream()
                .flatMap(course -> course.getCourseUserProgresses().stream())
                .map(userProgress -> userProgress.getUser())
                .collect(Collectors.toList());

        List<Message> userMessages = messageRepo.findAll();
        MessageDto messageDto = new MessageDto();
        model.addAttribute("userMessages", userMessages);
        model.addAttribute("tutors", students);
        model.addAttribute("message", messageDto);
        return "managerHome/tutorChat";
    }

    @PostMapping("/send-message")
    public String sendMessage(@ModelAttribute("message") MessageDto messageDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username).orElse(null);
        Message message = new Message();
        message.setContent(messageDto.getContent());
        message.setTimestamp(LocalDateTime.now());
        message.setSender(user);
        message.setReceiver(userRepository.findById(messageDto.getReceiver()).get());
        messageRepo.save(message);
        return "redirect:/manager/chat";
    }
}
