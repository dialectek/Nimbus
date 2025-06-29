# Name generator.
# See: https://github.com/aziele/unique-namer

import sys
import hashlib
import namer

name_filename = 'id_to_name.txt'
if len(sys.argv) != 2:
    sys.exit(1)
id = sys.argv[1]
encoded_id = id.encode('utf-8')
sha256_id = hashlib.sha256(encoded_id).hexdigest()
integer_id = int(sha256_id, 16)
name = namer.generate(random_seed=integer_id)
with open(name_filename, 'w') as f:
    f.write(name)
sys.exit(0)