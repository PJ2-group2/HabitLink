import os
import javalang
from graphviz import Digraph

def parse_java_files(root_dir):
    """
    Recursively parse all Java files in the given directory and extract class declarations,
    along with their fields and methods, and captures inheritance relationships.

    Returns:
        classes: A dictionary mapping class names to a dictionary of details with:
                    - "node": the javalang AST node,
                    - "fields": list of field dictionaries (each with name, type, and modifiers),
                    - "methods": list of method dictionaries (each with name, return type, and parameters).
        extends_edges: List of tuples (child, parent) representing inheritance (extends) relationships.
    """
    classes = {}          # Stores discovered classes and their extra details.
    extends_edges = []    # Inheritance relationships

    for subdir, _, files in os.walk(root_dir):
        for filename in files:
            if filename.endswith(".java"):
                file_path = os.path.join(subdir, filename)
                try:
                    with open(file_path, "r", encoding="utf-8") as f:
                        source_code = f.read()
                    tree = javalang.parse.parse(source_code)
                except Exception as e:
                    print(f"Skipping {file_path}: {e}")
                    continue

                # Process each class declared in the file.
                for _, node in tree.filter(javalang.tree.ClassDeclaration):
                    class_name = node.name
                    fields = []
                    methods = []
                    # Examine each member of the class body.
                    for member in node.body:
                        if isinstance(member, javalang.tree.FieldDeclaration):
                            # One declaration can contain multiple variables.
                            for declarator in member.declarators:
                                type_name = member.type.name if hasattr(member.type, "name") else str(member.type)
                                fields.append({
                                    "name": declarator.name,
                                    "type": type_name,
                                    "modifiers": member.modifiers
                                })
                        elif isinstance(member, javalang.tree.MethodDeclaration):
                            method_name = member.name
                            if member.return_type:
                                return_type = member.return_type.name if hasattr(member.return_type, "name") else str(member.return_type)
                            else:
                                return_type = "void"
                            parameters = []
                            for param in member.parameters:
                                param_type = param.type.name if hasattr(param.type, "name") else str(param.type)
                                parameters.append((param.name, param_type))
                            methods.append({
                                "name": method_name,
                                "return_type": return_type,
                                "parameters": parameters
                            })
                    classes[class_name] = {
                        "node": node,
                        "fields": fields,
                        "methods": methods,
                    }
                    # If the class extends a parent, record that relationship.
                    if node.extends:
                        parent_name = node.extends.name if hasattr(node.extends, "name") else str(node.extends)
                        extends_edges.append((class_name, parent_name))
    
    return classes, extends_edges

def build_relationship_edges(classes):
    """
    Analyze the gathered classes to build additional relationship edges.
    
    - **Association:** When a class has a field or a method parameter whose type is another
      class defined in the scanned project.
    - **Composition:** Here we use a simple heuristic: if a field is declared with both the 'private'
      and 'final' modifiers then it is flagged as composition (strong ownership).
    
    Returns:
        association_edges: List of (class, associated_class) tuples.
        composition_edges: List of (class, composed_class) tuples.
    """
    association_edges = set()
    composition_edges = set()

    # Check all fields for association/composition.
    for class_name, class_info in classes.items():
        for field in class_info.get("fields", []):
            field_type = field["type"]
            if field_type in classes:
                # If marked as 'private' and 'final', consider it a composition.
                if "private" in field["modifiers"] and "final" in field["modifiers"]:
                    composition_edges.add((class_name, field_type))
                else:
                    association_edges.add((class_name, field_type))
        # Also, method parameters that reference other classes are considered associations.
        for method in class_info.get("methods", []):
            for param_name, param_type in method["parameters"]:
                if param_type in classes:
                    association_edges.add((class_name, param_type))
    
    return list(association_edges), list(composition_edges)

def generate_dot(classes, extends_edges, association_edges, composition_edges, output_filename):
    """
    Generate a Graphviz class diagram to illustrate:
    
        - Inheritance relationships: represented with an arrow labeled "extends" (using an empty arrowhead).
        - Association relationships: depicted with a dashed edge labeled "association".
        - Composition relationships: depicted with a bold edge labeled "composition" with a diamond arrowhead.
    
    The diagram's nodes include detailed labels that show the class name, its fields, and its methods.
    """
    dot = Digraph(comment="Java Class Diagram", format="pdf")
    
    # Create nodes for each class, including detailed labels.
    for class_name, class_info in classes.items():
        # Prepare fields string; each field is rendered as "name: type"
        fields_str = ""
        if class_info["fields"]:
            for field in class_info["fields"]:
                fields_str += f"{field['name']}: {field['type']}\\l"
        else:
            fields_str = "None\\l"
        # Prepare methods string; each method shows its name, parameters, and return type.
        methods_str = ""
        if class_info["methods"]:
            for method in class_info["methods"]:
                params_str = ", ".join([f"{p[0]}: {p[1]}" for p in method["parameters"]])
                methods_str += f"{method['name']}({params_str}): {method['return_type']}\\l"
        else:
            methods_str = "None\\l"
        # Using record shape formatting, split into sections for class name, fields, and methods.
        label = f"{{ {class_name} | <F>Fields:\\l{fields_str} | <M>Methods:\\l{methods_str} }}"
        dot.node(class_name, label=label, shape="record")

    # Inheritance: draw an edge from the parent to the child.
    for child, parent in extends_edges:
        dot.edge(parent, child, label="extends", arrowhead="empty")

    # Association: draw a dashed edge for association relationships.
    for source, target in association_edges:
        # (Skip if there’s already an inheritance or composition edge.)
        if (source, target) not in composition_edges and (source, target) not in extends_edges:
            dot.edge(source, target, label="association", style="dashed")

    # Composition: draw a bold edge with a diamond arrowhead.
    for source, target in composition_edges:
        dot.edge(source, target, label="composition", arrowhead="diamond", style="bold")

    # Render the diagram to the specified output file.
    output_path = dot.render(output_filename, view=True)
    print(f"Diagram generated at: {output_path}")

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Generate a class diagram for a Java project")
    parser.add_argument("root_dir", help="Path to the root directory of your Java source code")
    parser.add_argument("output_file", help="Output file name for the generated diagram (without extension)")
    args = parser.parse_args()

    # Parse the Java source code to extract classes, methods, fields, and inheritance relationships.
    classes, extends_edges = parse_java_files(args.root_dir)
    
    # Analyze fields and method parameters to build association and composition relationships.
    association_edges, composition_edges = build_relationship_edges(classes)

    # Generate the complete class diagram and render it.
    generate_dot(classes, extends_edges, association_edges, composition_edges, args.output_file)
