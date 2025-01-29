# scheduler_python/people/type.py

from enum import Enum

class Type(Enum):
    """
    Translated from Java's Type enum.
    FO = IMS1
    BO = IMS2
    FO_AND_BO = either
    """
    FO = "IMS1"
    BO = "IMS2"
    FO_AND_BO = "IMS"

    def good_with(self, other):
        """
        Mirroring 'public boolean goodWith(Type type)' from Java.
        FO and FO is not good if both want FO.
        BO and BO is not good if both want BO.
        But FO_AND_BO is flexible.
        """
        if self == Type.FO_AND_BO:
            return True
        if other == Type.FO_AND_BO:
            return True
        # if both are the same and not FO_AND_BO => not good
        return self != other

    @staticmethod
    def is_first_fo(t1, t2):
        """
        Mirroring 'isFirstFo' from Java: 
         - if t1 = FO or t2 = BO => t1 is considered FO
        Actually the Java code does:
           return t1.equals(FO) || t2.equals(BO);
        So we replicate that.
        """
        return (t1 == Type.FO) or (t2 == Type.BO)

    def to_cell(self):
        return self.value  # e.g. "IMS1", "IMS2", or "IMS"
