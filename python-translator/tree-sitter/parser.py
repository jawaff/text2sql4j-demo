from tree_sitter import Language, Parser
from tree_sitter.binding import Node, Parser, Tree, TreeCursor

from tree_sitter_languages import get_language, get_parser

language = get_language('sql')
parser = get_parser('sql')

def is_valid_tree(cursor: TreeCursor):
    print(f'Node: {cursor.text} - {cursor.current_field_name()}')
    #print(f'isNamed={node.is_named} isMissing={node.is_missing} hasError={node.has_error}')
    if cursor.type == 'ERROR':
        return False
    is_valid = True
    for n in cursor.children:
        if not is_valid_tree(n):
            is_valid = False
    return is_valid

def traverse_tree(tree: Tree):
    cursor = tree.walk()
    reached_root = False
    while reached_root == False:
        yield cursor.current_field_name(), cursor.node

        if cursor.goto_first_child():
            continue
        if cursor.goto_next_sibling():
            continue

        retracing = True
        while retracing:
            if not cursor.goto_parent():
                retracing = False
                reached_root = True
            if cursor.goto_next_sibling():
                retracing = False

def check_top_token(cur_tree: Tree, top_token):
    cur_length = len(cur_tree.text)
    new_length = cur_length + 1 + len(top_token)
    print(f'cur_length: {cur_length} new_length: {new_length}')
    #cur_tree.edit(
    #    start_byte=cur_length,
    #    old_end_byte=cur_length,
    #    new_end_byte=new_length,
    #    start_point=(0, cur_length),
    #    old_end_point=(0, cur_length),
    #    new_end_point=(0, new_length),
    #)
    new_tree = parser.parse(bytes(f'{top_token}', "utf-8"), cur_tree)
    print(new_tree.root_node.sexp())

    is_valid = True
    for name, node in traverse_tree(new_tree):
        print(name)
        print(node)
        if node.type == 'ERROR':
            is_valid = False
            break
    return is_valid, new_tree


#tree2 = parser.parse(bytes("totally incorrect SQL 5000!!@#$%", "utf-8"))
#print(tree2.text)
#print(tree2.root_node.children)
#print('1')
#print(tree2.root_node)
#traverse_tree(tree2.root_node)
#print(tree2.root_node.type)
#print(missing_nodes)
#missing_nodes.clear()

#input = 'SELECT * FROM concert t1 WHERE t1.name = \"dog\"'
top_tokens = ['', ' FROM', ' t1.name = "dog"']

cur_tree = parser.parse(bytes('SELECT * FROM concert WHERE ', 'utf-8'))
valid_trees = []
for top_token in top_tokens:
    print(cur_tree.text)
    #print(dir(cur_tree))
    is_valid, new_tree = check_top_token(cur_tree, top_token)
    print(new_tree.text)
    if is_valid:
        valid_trees.append(new_tree.text)

print(valid_trees)

#tree = parser.parse(bytes(, "utf-8"))
#print(dir(tree))
#print(tree.text)
#print(tree.root_node.children)
#print('2')
#print(tree.root_node)
#traverse_tree(tree.root_node)
#print(missing_nodes)
#missing_nodes.clear()

#new_tree = parser.parse(bytes(" WHERE t1.name = \"", "utf-8"), tree)
#print(new_tree)
#print(new_tree.root_node.children)
#print('3')
#print(new_tree.root_node)
#traverse_tree(new_tree.root_node)
#print(missing_nodes)
#missing_nodes.clear()