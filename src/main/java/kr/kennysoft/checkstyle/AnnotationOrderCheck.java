package kr.kennysoft.checkstyle;

import com.puppycrawl.tools.checkstyle.StatelessCheck;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @see com.puppycrawl.tools.checkstyle.checks.modifier.ModifierOrderCheck
 */
@StatelessCheck
public class AnnotationOrderCheck extends AbstractCheck {

    private static final String[] ORDER_FOR_CLASS = {
            // Java
            "Deprecated",

            // Spring
            "Profile",
            "SpringBootApplication",
            "Controller", "RestController", "ControllerAdvice", "Service", "Component", "Configuration",
            "Aspect", "Converter",
            // TODO: Enable 시리즈

            // JPA
            "Entity", "Embeddable",
            "Table",
            "IdClass",
            "BatchSize",
            "EntityListeners",
            "Audited",
            "AuditTable",

            // Test
            "Disabled",
            "ActiveProfiles",
            "ExtendWith",
            "SpringBootTest",
            "TestConfiguration",
            "Transactional",

            // Jackson
            "JsonSerialize",
            "JsonFormat",

            // Lombok
            "NoArgsConstructor", "AllArgsConstructor", "RequiredArgsConstructor",
            "Builder",
            "Getter", "Setter", "Data",
            "EqualsAndHashCode",
            "ToString",
            "Slf4j",

            // Checkstyle
            "StatelessCheck",
    };

    private static final String[] ORDER_FOR_INTERFACE = {
            // Spring
            "Repository",
            "FeignClient",

            // Lombok
            "Slf4j",
    };

    private static final String[] ORDER_FOR_METHOD = {
            // Java
            "Override",
            "Deprecated",
            "SuppressWarnings",

            // Spring
            "PreAuthorize",
            "GetMapping", "PostMapping", "PutMapping", "PatchMapping", "DeleteMapping",
            "ExceptionHandler",
            "ResponseStatus", "ResponseBody",
            "Around",
            "Async",
            "Cacheable",
            "Primary",
            "Bean",
            "ConfigurationProperties",

            // validation
            "AssertTrue",

            // JUnit
            "Disabled",
            "WithMockUser", "WithUserDetails",
            "BeforeAll", "BeforeEach", "AfterAll", "AfterEach",
            "Test",

            // schedule
            "Scheduled",
            "SchedulerLock",

            // JPA
            "Transactional",
            "Modifying",
            "Query",
            "EntityGraph",

            // Jackson
            "JsonCreator",
            "JsonIgnore",

            // springdoc
            "Operation",
    };

    private static final String[] ORDER_FOR_FIELD = {
            // Java
            "Deprecated",
            "SuppressWarnings",

            // Spring
            "Value",
            "Qualifier",

            // validation
            "NotNull", "NotBlank", "NotEmpty",
            "Size",
            "Min", "Max",
            "Email",

            // Test
            "LocalServerPort",
            "Spy",
            "InjectMocks",
            "Mock",

            // JPA
            "Embedded",
            "Id",
            "GeneratedValue",
            "Fetch",
            "OneToOne", "OneToMany", "ManyToOne",
            "CreatedDate", "CreatedBy", "LastModifiedDate", "LastModifiedBy",
            "Enumerated",
            "Convert",
            "Column", "JoinColumn",
            "Formula",
            "BatchSize",
            "Where",
            "OrderBy",

            // Jackson
            "JsonProperty",
            "JsonFormat",
            "JsonIgnore",

            // springdoc
            "Schema",

            // Lombok
            "Setter",
    };

    private static final String[] ORDER_FOR_PARAMETER = {
            // validation
            "Valid",
            "NotNull",

            // Spring
            "PathVariable", "RequestParam", "RequestBody", "RequestPart", "ModelAttribute", "CookieValue",
            "PageableDefault", "SortDefault",
            "Qualifier",

            // JPA
            "Param",

            // Jackson
            "JsonProperty",

            // springdoc
            "ParameterObject", "AuthenticationPrincipal",
    };

    private static final Map<Integer, String[]> ORDER_FOR_TYPE;
    private static final Map<Integer, Set<String>> ORDERED_ANNOTATIONS_FOR_TYPE;

    static {
        ORDER_FOR_TYPE = Map.ofEntries(
                Map.entry(TokenTypes.CLASS_DEF, ORDER_FOR_CLASS),
                Map.entry(TokenTypes.ENUM_DEF, ORDER_FOR_CLASS),
                Map.entry(TokenTypes.INTERFACE_DEF, ORDER_FOR_INTERFACE),
                Map.entry(TokenTypes.METHOD_DEF, ORDER_FOR_METHOD),
                Map.entry(TokenTypes.CTOR_DEF, ORDER_FOR_METHOD),
                Map.entry(TokenTypes.VARIABLE_DEF, ORDER_FOR_FIELD),
                Map.entry(TokenTypes.PARAMETER_DEF, ORDER_FOR_PARAMETER)
        );

        ORDERED_ANNOTATIONS_FOR_TYPE = ORDER_FOR_TYPE.keySet().stream().collect(Collectors.toMap(
                Function.identity(),
                k -> Stream.of(ORDER_FOR_TYPE.get(k)).collect(Collectors.toSet()))
        );
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {TokenTypes.MODIFIERS};
    }

    @Override
    public void visitToken(DetailAST ast) {
        final List<DetailAST> annotations = new ArrayList<>();
        DetailAST annotation = ast.getFirstChild();
        while (annotation != null) {
            if (annotation.getType() == TokenTypes.ANNOTATION) {
                annotations.add(annotation.findFirstToken(TokenTypes.IDENT));
            }
            annotation = annotation.getNextSibling();
        }

        if (!annotations.isEmpty()) {
            String[] order = ORDER_FOR_TYPE.get(ast.getParent().getType());
            Set<String> orderedAnnotations = ORDERED_ANNOTATIONS_FOR_TYPE.get(ast.getParent().getType());
            if (order == null || orderedAnnotations == null) {
                log(ast,
                        "Modifiers of type ''{0}'' not supported.",
                        ast.getParent().getText());
                return;
            }
            final DetailAST error = checkOrder(annotations, order, orderedAnnotations);
            if (error != null) {
                log(error,
                        "''{0}'' annotation out of order.",
                        error.getText());
            }
        }
    }

    private DetailAST checkOrder(List<DetailAST> modifiers, String[] order, Set<String> orderedAnnotations) {
        final Iterator<DetailAST> iterator = modifiers.iterator();

        DetailAST annotation = iterator.next();

        DetailAST offendingAnnotation = null;

        int index = 0;

        while (annotation != null
                && offendingAnnotation == null) {
            String annotationIdent = annotation.getText();
            if (orderedAnnotations.contains(annotationIdent)) {
                while (index < order.length
                        && !order[index].equals(annotationIdent)) {
                    index++;
                }
            } else {
                String annotationOf = annotation.getParent() // ANNOTATION
                        .getParent() // MODIFIERS
                        .getParent().getText();
                if (!(annotationOf.equals("CLASS_DEF") && annotationIdent.startsWith("Enable"))) {
                    log(annotation,
                            "''{0}'' annotation order not configured for {1}.",
                            annotationIdent, annotationOf);
                }
            }

            if (index == order.length) {
                // Current annotation is out of order
                offendingAnnotation = annotation;
            } else if (iterator.hasNext()) {
                annotation = iterator.next();
            } else {
                // Reached end of annotations without problem
                annotation = null;
            }
        }
        return offendingAnnotation;
    }

}
