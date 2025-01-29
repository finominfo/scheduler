# scheduler_python/common/globals.py

class Globals:
    """
    A direct translation of the Java Globals class.
    In Java, it uses a singleton pattern. Here we can just have a simple class or constants.
    """
    def __init__(self):
        # Mirroring the Java file names:
        self.config_file = "config.csv"
        self.result_file = "result.csv"

    def get_config_file(self):
        return self.config_file

    def get_result_file(self):
        return self.result_file
