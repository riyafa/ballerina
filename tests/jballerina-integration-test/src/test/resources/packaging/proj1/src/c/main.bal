import a;
import b;

function init() {
   a:incrementCount();
   a:assertCount(3);
}

public function main() {
    b:sample();
}

listener a:ABC ep = new a:ABC("ModC");
