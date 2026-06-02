"""
Security Audit Framework for JNexus

This module provides a comprehensive framework for conducting third-party security audits
of the JNexus application, which handles sensitive credentials and performs destructive operations.
The framework supports multiple auditor types, generates detailed reports, and tracks vulnerabilities
throughout the audit lifecycle.
"""

from __future__ import annotations

import enum
import logging
import re
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Sequence, Tuple, Union
from uuid import UUID, uuid4

# Configure module logger
logger = logging.getLogger(__name__)

# Constants
MAX_SCORE: float = 100.0
MIN_SCORE: float = 0.0
MAX_CVSS_SCORE: float = 10.0
MIN_CVSS_SCORE: float = 0.0
MAX_DESCRIPTION_LENGTH: int = 5000
MAX_NAME_LENGTH: int = 200
MIN_DURATION_DAYS: int = 1
MAX_DURATION_DAYS: int = 365


class SecurityGrade(enum.Enum):
    """Enumeration of possible security grades with associated score ranges."""
    
    A_PLUS = "A+"
    A = "A"
    B = "B"
    C = "C"
    D = "D"
    F = "F"
    
    @classmethod
    def from_score(cls, score: float) -> SecurityGrade:
        """
        Determine security grade from numeric score.
        
        Args:
            score: Numeric score between 0 and 100
            
        Returns:
            Corresponding SecurityGrade
            
        Raises:
            ValueError: If score is outside valid range
        """
        if not MIN_SCORE <= score <= MAX_SCORE:
            raise ValueError(f"Score must be between {MIN_SCORE} and {MAX_SCORE}, got {score}")
        
        if score >= 99:
            return cls.A_PLUS
        elif score >= 90:
            return cls.A
        elif score >= 80:
            return cls.B
        elif score >= 70:
            return cls.C
        elif score >= 60:
            return cls.D
        else:
            return cls.F


class SeverityLevel(enum.Enum):
    """Enumeration of vulnerability severity levels with CVSS score ranges."""
    
    CRITICAL = "Critical"
    HIGH = "High"
    MEDIUM = "Medium"
    LOW = "Low"
    INFORMATIONAL = "Informational"
    
    @classmethod
    def from_cvss_score(cls, cvss_score: float) -> SeverityLevel:
        """
        Determine severity level from CVSS score.
        
        Args:
            cvss_score: CVSS score between 0 and 10
            
        Returns:
            Corresponding SeverityLevel
            
        Raises:
            ValueError: If CVSS score is outside valid range
        """
        if not MIN_CVSS_SCORE <= cvss_score <= MAX_CVSS_SCORE:
            raise ValueError(f"CVSS score must be between {MIN_CVSS_SCORE} and {MAX_CVSS_SCORE}, got {cvss_score}")
        
        if cvss_score >= 9.0:
            return cls.CRITICAL
        elif cvss_score >= 7.0:
            return cls.HIGH
        elif cvss_score >= 4.0:
            return cls.MEDIUM
        elif cvss_score >= 0.1:
            return cls.LOW
        else:
            return cls.INFORMATIONAL


class AuditPhase(enum.Enum):
    """Enumeration of audit phases in chronological order."""
    
    PRE_AUDIT_PREPARATION = "Pre-Audit Preparation"
    STATIC_ANALYSIS = "Static Analysis"
    DYNAMIC_TESTING = "Dynamic Testing"
    REPORT_GENERATION = "Report Generation"
    REMEDIATION = "Remediation"
    RETESTING = "Retesting"
    FINAL_REPORT = "Final Report"
    
    @property
    def order(self) -> int:
        """Get the chronological order of this phase."""
        phase_order = {
            AuditPhase.PRE_AUDIT_PREPARATION: 0,
            AuditPhase.STATIC_ANALYSIS: 1,
            AuditPhase.DYNAMIC_TESTING: 2,
            AuditPhase.REPORT_GENERATION: 3,
            AuditPhase.REMEDIATION: 4,
            AuditPhase.RETESTING: 5,
            AuditPhase.FINAL_REPORT: 6
        }
        return phase_order[self]


class AuditorType(enum.Enum):
    """Enumeration of auditor types with associated cost estimates."""
    
    OWASP = "OWASP Security Audit Project"
    PROFESSIONAL_FIRM = "Professional Security Firm"
    BUG_BOUNTY = "Bug Bounty Program"
    
    @property
    def cost_estimate(self) -> str:
        """Get cost estimate for this auditor type."""
        cost_estimates = {
            AuditorType.OWASP: "Free/Low-cost",
            AuditorType.PROFESSIONAL_FIRM: "$15,000 - $50,000",
            AuditorType.BUG_BOUNTY: "Variable (pay per finding)"
        }
        return cost_estimates[self]


@dataclass(frozen=True)
class SecurityScore:
    """
    Immutable data class representing a security score.
    
    Attributes:
        grade: Security grade (A+, A, B, etc.)
        score: Numeric score (0-100)
        timestamp: When the score was calculated
    """
    
    grade: SecurityGrade
    score: float
    timestamp: datetime = field(default_factory=datetime.utcnow)
    
    def __post_init__(self) -> None:
        """Validate score range after initialization."""
        if not MIN_SCORE <= self.score <= MAX_SCORE:
            raise ValueError(f"Score must be between {MIN_SCORE} and {MAX_SCORE}, got {self.score}")
        logger.debug(f"SecurityScore created: grade={self.grade.value}, score={self.score}")
    
    def to_dict(self) -> Dict[str, Union[str, float, str]]:
        """
        Convert to dictionary representation.
        
        Returns:
            Dictionary with grade, score, and timestamp
        """
        return {
            "grade": self.grade.value,
            "score": self.score,
            "timestamp": self.timestamp.isoformat()
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Union[str, float]]) -> SecurityScore:
        """
        Create SecurityScore from dictionary.
        
        Args:
            data: Dictionary with grade, score, and optional timestamp
            
        Returns:
            New SecurityScore instance
            
        Raises:
            ValueError: If required fields are missing or invalid
        """
        try:
            grade = SecurityGrade(data["grade"])
            score = float(data["score"])
            timestamp = datetime.fromisoformat(data.get("timestamp", datetime.utcnow().isoformat()))
            return cls(grade=grade, score=score, timestamp=timestamp)
        except (KeyError, ValueError, TypeError) as e:
            logger.error(f"Failed to create SecurityScore from dict: {e}")
            raise ValueError(f"Invalid SecurityScore data: {e}")


@dataclass(frozen=True)
class AuditScope:
    """
    Immutable data class defining the scope of a security audit.
    
    Attributes:
        credential_security: Whether to audit credential security
        input_validation: Whether to audit input validation
        destructive_operations: Whether to audit destructive operations
        network_security: Whether to audit network security
        authentication: Whether to audit authentication/authorization
        data_protection: Whether to audit data protection
        dependency_security: Whether to audit dependency security
    """
    
    credential_security: bool = True
    input_validation: bool = True
    destructive_operations: bool = True
    network_security: bool = True
    authentication: bool = True
    data_protection: bool = True
    dependency_security: bool = True
    
    def __post_init__(self) -> None:
        """Validate that at least one scope area is selected."""
        if not any([self.credential_security, self.input_validation, self.destructive_operations,
                    self.network_security, self.authentication, self.data_protection,
                    self.dependency_security]):
            raise ValueError("At least one audit scope area must be selected")
        logger.debug(f"AuditScope created with {len(self.to_list())} active areas")
    
    def to_list(self) -> List[str]:
        """
        Convert scope to list of active audit areas.
        
        Returns:
            List of active audit area names
        """
        areas: List[str] = []
        if self.credential_security:
            areas.append("Credential Security")
        if self.input_validation:
            areas.append("Input Validation")
        if self.destructive_operations:
            areas.append("Destructive Operations")
        if self.network_security:
            areas.append("Network Security")
        if self.authentication:
            areas.append("Authentication & Authorization")
        if self.data_protection:
            areas.append("Data Protection")
        if self.dependency_security:
            areas.append("Dependency Security")
        return areas
    
    def to_dict(self) -> Dict[str, bool]:
        """
        Convert scope to dictionary representation.
        
        Returns:
            Dictionary with scope area names as keys and boolean values
        """
        return {
            "credential_security": self.credential_security,
            "input_validation": self.input_validation,
            "destructive_operations": self.destructive_operations,
            "network_security": self.network_security,
            "authentication": self.authentication,
            "data_protection": self.data_protection,
            "dependency_security": self.dependency_security
        }


@dataclass(frozen=True)
class Vulnerability:
    """
    Immutable data class representing a discovered vulnerability.
    
    Attributes:
        id: Unique identifier for the vulnerability
        severity: Severity level
        description: Description of the vulnerability
        affected_components: List of affected components
        cve_id: Optional CVE identifier
        cvss_score: Optional CVSS score
        proof_of_concept: Optional proof of concept
        remediation: Optional remediation steps
        file_path: Optional file path where vulnerability was found
        line_number: Optional line number
    """
    
    id: UUID
    severity: SeverityLevel
    description: str
    affected_components: List[str]
    cve_id: Optional[str] = None
    cvss_score: Optional[float] = None
    proof_of_concept: Optional[str] = None
    remediation: Optional[str] = None
    file_path: Optional[str] = None
    line_number: Optional[int] = None
    
    def __post_init__(self) -> None:
        """Validate vulnerability data after initialization."""
        # Validate description
        if not self.description or not self.description.strip():
            raise ValueError("Description cannot be empty")
        if len(self.description) > MAX_DESCRIPTION_LENGTH:
            raise ValueError(f"Description exceeds maximum length of {MAX_DESCRIPTION_LENGTH}")
        
        # Validate affected components
        if not self.affected_components:
            raise ValueError("At least one affected component is required")
        for component in self.affected_components:
            if not component or not component.strip():
                raise ValueError("Affected component cannot be empty")
        
        # Validate CVSS score
        if self.cvss_score is not None and not MIN_CVSS_SCORE <= self.cvss_score <= MAX_CVSS_SCORE:
            raise ValueError(f"CVSS score must be between {MIN_CVSS_SCORE} and {MAX_CVSS_SCORE}, got {self.cvss_score}")
        
        # Validate CVE ID format if provided
        if self.cve_id is not None:
            cve_pattern = re.compile(r'^CVE-\d{4}-\d{4,}$')
            if not cve_pattern.match(self.cve_id):
                raise ValueError(f"Invalid CVE ID format: {self.cve_id}")
        
        # Validate line number
        if self.line_number is not None and self.line_number < 0:
            raise ValueError(f"Line number cannot be negative, got {self.line_number}")
        
        logger.debug(f"Vulnerability created: id={self.id}, severity={self.severity.value}")
    
    def to_dict(self) -> Dict[str, Union[str, float, int, List[str], None]]:
        """
        Convert to dictionary representation.
        
        Returns:
            Dictionary with all vulnerability fields
        """
        return {
            "id": str(self.id),
            "severity": self.severity.value,
            "description": self.description,
            "affected_components": self.affected_components,
            "cve_id": self.cve_id,
            "cvss_score": self.cvss_score,
            "proof_of_concept": self.proof_of_concept,
            "remediation": self.remediation,
            "file_path": self.file_path,
            "line_number": self.line_number
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Union[str, float, int, List[str], None]]) -> Vulnerability:
        """
        Create Vulnerability from dictionary.
        
        Args:
            data: Dictionary with vulnerability fields
            
        Returns:
            New Vulnerability instance
            
        Raises:
            ValueError: If required fields are missing or invalid
        """
        try:
            vuln_id = UUID(data["id"]) if isinstance(data.get("id"), str) else uuid4()
            severity = SeverityLevel(data["severity"])
            description = str(data["description"])
            affected_components = list(data["affected_components"])
            cve_id = str(data["cve_id"]) if data.get("cve_id") else None
            cvss_score = float(data["cvss_score"]) if data.get("cvss_score") is not None else None
            proof_of_concept = str(data["proof_of_concept"]) if data.get("proof_of_concept") else None
            remediation = str(data["remediation"]) if data.get("remediation") else None
            file_path = str(data["file_path"]) if data.get("file_path") else None
            line_number = int(data["line_number"]) if data.get("line_number") is not None else None
            
            return cls(
                id=vuln_id,
                severity=severity,
                description=description,
                affected_components=affected_components,
                cve_id=cve_id,
                cvss_score=cvss_score,
                proof_of_concept=proof_of_concept,
                remediation=remediation,
                file_path=file_path,
                line_number=line_number
            )
        except (KeyError, ValueError, TypeError) as e:
            logger.error(f"Failed to create Vulnerability from dict: {e}")
            raise ValueError(f"Invalid Vulnerability data: {e}")


@dataclass(frozen=True)
class AuditDeliverable:
    """
    Immutable data class representing an audit deliverable.
    
    Attributes:
        name: Name of the deliverable
        description: Description of the deliverable
        content_type: Type of content (e.g., PDF, JSON, XML)
        generated_at: When the deliverable was generated
    """
    
    name: str
    description: str
    content_type: str
    generated_at: datetime = field(default_factory=datetime.utcnow)
    
    def __post_init__(self) -> None:
        """Validate deliverable data after initialization."""
        if not self.name or not self.name.strip():
            raise ValueError("Deliverable name cannot be empty")
        if len(self.name) > MAX_NAME_LENGTH:
            raise ValueError(f"Deliverable name exceeds maximum length of {MAX_NAME_LENGTH}")
        
        if not self.description or not self.description.strip():
            raise ValueError("Deliverable description cannot be empty")
        
        if not self.content_type or not self.content_type.strip():
            raise ValueError("Content type cannot be empty")
        
        valid_content_types = {"PDF", "JSON", "XML", "HTML", "CSV", "TXT", "DOCX", "XLSX"}
        if self.content_type.upper() not in valid_content_types:
            logger.warning(f"Unusual content type: {self.content_type}")
        
        logger.debug(f"AuditDeliverable created: name={self.name}, type={self.content_type}")
    
    def to_dict(self) -> Dict[str, Union[str, datetime]]:
        """
        Convert to dictionary representation.
        
        Returns:
            Dictionary with deliverable fields
        """
        return {
            "name": self.name,
            "description": self.description,
            "content_type": self.content_type,
            "generated_at": self.generated_at.isoformat()
        }


@dataclass(frozen=True)
class AuditTimeline:
    """
    Immutable data class representing the audit timeline.
    
    Attributes:
        phase: Current audit phase
        duration_days: Duration in days for this phase
        activities: List of activities in this phase
        start_date: When the phase starts
        end_date: When the phase ends
    """
    
    phase: AuditPhase
    duration_days: int
    activities: List[str]
    start_date: Optional[datetime] = None
    end_date: Optional[datetime] = None
    
    def __post_init__(self) -> None:
        """Validate timeline data after initialization."""
        if self.duration_days < MIN_DURATION_DAYS:
            raise ValueError(f"Duration must be at least {MIN_DURATION_DAYS} day, got {self.duration_days}")
        if self.duration_days > MAX_DURATION_DAYS:
            raise ValueError(f"Duration cannot exceed {MAX_DURATION_DAYS} days, got {self.duration_days}")
        
        if not self.activities:
            raise ValueError("At least one activity is required")
        for activity in self.activities:
            if not activity or not activity.strip():
                raise ValueError("Activity cannot be empty")
        
        if self.start_date and self.end_date and self.start_date >= self.end_date:
            raise ValueError("Start date must be before end date")
        
        logger.debug(f"AuditTimeline created: phase={self.phase.value}, duration={self.duration_days}d")
    
    def calculate_end_date(self) -> Optional[datetime]:
        """
        Calculate end date based on start date and duration.
        
        Returns:
            Calculated end date or None if start date is not set
        """
        if self.start_date is None:
            logger.warning("Cannot calculate end date: start date is not set")
            return None
        
        end_date = self.start_date + timedelta(days=self.duration_days)
        logger.debug(f"Calculated end date: {end_date.isoformat()}")
        return end_date
    
    def to_dict(self) -> Dict[str, Union[str,