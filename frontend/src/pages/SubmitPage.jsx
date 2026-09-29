import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { createProject } from '../api/projectApi';
import { extractError } from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';
import Alert from '../components/Alert';
import {
  CUSTOM_DOMAIN,
  DEPARTMENT_DOMAINS,
  DEPARTMENTS,
  ACADEMIC_YEARS,
  PROJECT_YEARS,
  SAMPLE_IMAGES,
  getCoverOptions,
  imageUrl,
  isValidAcademicYear,
} from '../constants';

const DEPLOY_LINK_REQUIRED_DEPARTMENTS = ['CSE', 'IT'];
const OPTIONAL_RESOURCE_DEPARTMENTS = ['ECE', 'EEE', 'Civil', 'Mechanical'];

export default function SubmitPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const initialDepartment = user?.department || 'CSE';
  const initialDomain = DEPARTMENT_DOMAINS[initialDepartment]?.[0] || CUSTOM_DOMAIN;

  const [step, setStep] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [serverError, setServerError] = useState('');
  const [errors, setErrors] = useState({});
  const [coverFile, setCoverFile] = useState(null);
  const [coverPreview, setCoverPreview] = useState('');
  const [coverError, setCoverError] = useState('');

  const [form, setForm] = useState({
    title: '',
    description: '',
    deployLink: '',
    department: initialDepartment,
    category: initialDomain,
    otherCategory: '',
    year: new Date().getFullYear(),
    academicYear: user?.academicYear || '',
    technologies: '',
    image: getCoverOptions(initialDepartment, initialDomain)[0]?.id || SAMPLE_IMAGES[0],
    supportingFiles: [],
    mediaFiles: [],
    teamMembers: [{ name: user?.name || '', rollNo: user?.rollNo || '' }],
  });

  const deployLinkRequired = DEPLOY_LINK_REQUIRED_DEPARTMENTS.includes(form.department);
  const showOptionalResources = OPTIONAL_RESOURCE_DEPARTMENTS.includes(form.department);
  const departmentDomains = DEPARTMENT_DOMAINS[form.department] || [];
  const isCustomDomain = form.category === CUSTOM_DOMAIN;

  useEffect(() => () => {
    if (coverPreview) URL.revokeObjectURL(coverPreview);
  }, [coverPreview]);

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function changeDepartment(department) {
    const category = DEPARTMENT_DOMAINS[department]?.[0] || CUSTOM_DOMAIN;
    const image = getCoverOptions(department, category)[0]?.id || SAMPLE_IMAGES[0];
    setForm((prev) => ({ ...prev, department, category, otherCategory: '', image }));
    setCoverFile(null);
    setCoverPreview('');
    setCoverError('');
  }

  function changeDomain(category) {
    const image = getCoverOptions(form.department, category)[0]?.id || SAMPLE_IMAGES[0];
    setForm((prev) => ({ ...prev, category, otherCategory: '', image }));
    setCoverFile(null);
    setCoverPreview('');
    setCoverError('');
  }

  function selectBuiltInCover(image) {
    setForm((prev) => ({ ...prev, image }));
    setCoverFile(null);
    setCoverPreview('');
    setCoverError('');
  }

  async function handleCoverUpload(event) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    const validationMessage = await validateCoverImage(file);
    if (validationMessage) {
      setCoverFile(null);
      setCoverPreview('');
      setCoverError(validationMessage);
      return;
    }

    setCoverError('');
    setCoverFile(file);
    setCoverPreview(URL.createObjectURL(file));
  }

  function updateMember(index, field, value) {
    setForm((prev) => ({
      ...prev,
      teamMembers: prev.teamMembers.map((m, i) => (i === index ? { ...m, [field]: value } : m)),
    }));
  }

  function addMember() {
    if (form.teamMembers.length >= 5) return;
    setForm((prev) => ({
      ...prev,
      teamMembers: [...prev.teamMembers, { name: '', rollNo: '' }],
    }));
  }

  function removeMember(index) {
    setForm((prev) => ({
      ...prev,
      teamMembers: prev.teamMembers.filter((_, i) => i !== index),
    }));
  }

  function validateStep(current) {
    const next = {};

    if (current === 1) {
      if (!form.title.trim()) next.title = 'Title is required';
      else if (form.title.trim().length < 5) next.title = 'Title must be at least 5 characters';

      if (!form.description.trim()) next.description = 'Description is required';
      else if (form.description.trim().length < 20)
        next.description = 'Description must be at least 20 characters';

      if (deployLinkRequired && !form.deployLink.trim()) {
        next.deployLink = 'Deploy link is required for CSE and IT projects';
      } else if (form.deployLink.trim()) {
        try {
          const deployUrl = new URL(form.deployLink.trim());
          if (!['http:', 'https:'].includes(deployUrl.protocol)) throw new Error();
        } catch {
          next.deployLink = 'Enter a valid URL starting with http:// or https://';
        }
      }

      if (isCustomDomain && !form.otherCategory.trim()) {
        next.category = 'Enter a project domain';
      } else if (isCustomDomain && form.otherCategory.trim().length > 40) {
        next.category = 'Project domain must be 40 characters or fewer';
      }

      if (!isValidAcademicYear(form.academicYear)) {
        next.academicYear = 'Enter an academic year range like 2023-2027';
      }
    }

    if (current === 2) {
      const techs = form.technologies
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean);
      if (techs.length === 0) next.technologies = 'Add at least one technology';

      const named = form.teamMembers.filter((m) => m.name.trim());
      if (named.length === 0) next.teamMembers = 'Add at least one team member';
    }

    setErrors(next);
    return Object.keys(next).length === 0;
  }

  function goNext() {
    if (validateStep(step)) setStep((s) => s + 1);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setServerError('');

    if (!validateStep(1) || !validateStep(2)) {
      setStep(1);
      return;
    }

    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      deployLink: form.deployLink.trim() || null,
      department: form.department,
      category: isCustomDomain ? form.otherCategory.trim() : form.category,
      customCategory: isCustomDomain,
      year: Number(form.year),
      academicYear: form.academicYear.trim(),
      image: form.image,
      technologies: form.technologies
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean),
      teamMembers: form.teamMembers
        .filter((m) => m.name.trim())
        .map((m) => ({ name: m.name.trim(), rollNo: m.rollNo.trim() })),
    };

    const formData = new FormData();
    formData.append('project', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    form.supportingFiles.forEach((file) => formData.append('supportingFiles', file));
    form.mediaFiles.forEach((file) => formData.append('mediaFiles', file));
    if (coverFile) formData.append('coverImage', coverFile);

    setSubmitting(true);
    try {
      await createProject(formData);
      setSubmitted(true);
    } catch (err) {
      setServerError(extractError(err, 'Could not submit the project.'));
    } finally {
      setSubmitting(false);
    }
  }

  if (submitted) {
    return (
      <div className="page-narrow empty-state">
        <div className="empty-icon">🎉</div>
        <h2>Project submitted</h2>
        <p>
          Your project has been sent for admin review. It will appear publicly once approved.
        </p>
        <div className="button-row">
          <Link to="/browse" className="btn btn-navy">
            Browse Projects
          </Link>
          <Link to="/profile" className="btn btn-outline">
            My Profile
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="page-banner">
        <div className="container">
          <h1>Submit Your Project</h1>
          <p>Share your work with the ACCET community</p>
          <div className="stepper">
            {[1, 2, 3].map((s) => (
              <div key={s} className="stepper-item">
                <span className={s <= step ? 'step-dot active' : 'step-dot'}>
                  {s < step ? '✓' : s}
                </span>
                {s < 3 && <span className={s < step ? 'step-line active' : 'step-line'} />}
              </div>
            ))}
            <span className="muted-light small">Step {step} of 3</span>
          </div>
        </div>
      </div>

      <div className="page-narrow section">
        <form className="panel" onSubmit={handleSubmit} noValidate>
          <Alert message={serverError} onClose={() => setServerError('')} />

          {step === 1 && (
            <div className="form-step">
              <h2 className="step-title">Basic information</h2>

              <label className="field">
                <span>Project Title *</span>
                <input
                  type="text"
                  value={form.title}
                  onChange={(e) => update('title', e.target.value)}
                  placeholder="e.g. Smart Attendance System using Face Recognition"
                />
                {errors.title && <small className="field-error">{errors.title}</small>}
              </label>

              <label className="field">
                <span>Description *</span>
                <textarea
                  rows={6}
                  value={form.description}
                  onChange={(e) => update('description', e.target.value)}
                  placeholder="What problem does it solve, how does it work, what did you build?"
                />
                {errors.description && (
                  <small className="field-error">{errors.description}</small>
                )}
              </label>

              <label className="field">
                <span>Project Deploy Link {deployLinkRequired ? '*' : '(optional)'}</span>
                <input
                  type="url"
                  value={form.deployLink}
                  onChange={(e) => update('deployLink', e.target.value)}
                  placeholder="https://your-project.example.com"
                />
                {!deployLinkRequired && (
                  <small className="hint">Optional for {form.department} projects</small>
                )}
                {errors.deployLink && <small className="field-error">{errors.deployLink}</small>}
              </label>

              <label className="field">
                <span>Supporting Files (optional)</span>
                <input
                  type="file"
                  multiple
                  onChange={(e) => update('supportingFiles', Array.from(e.target.files))}
                />
                <small className="hint">
                  Add files in any format, including PPT, PDF, and Word documents
                </small>
              </label>

              {showOptionalResources && (
                <label className="field">
                  <span>Images or Video (optional)</span>
                  <input
                    type="file"
                    accept="image/*,video/*"
                    multiple
                    onChange={(e) => update('mediaFiles', Array.from(e.target.files))}
                  />
                  <small className="hint">Add project images or demonstration videos</small>
                </label>
              )}

              <div className="field-row">
                <label className="field">
                  <span>Department *</span>
                  <select
                    value={form.department}
                    onChange={(e) => changeDepartment(e.target.value)}
                  >
                    {DEPARTMENTS.map((d) => (
                      <option key={d} value={d}>
                        {d}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="field">
                  <span>Project Domain *</span>
                  <select
                    value={form.category}
                    onChange={(e) => changeDomain(e.target.value)}
                  >
                    {departmentDomains.map((domain) => (
                      <option key={domain} value={domain}>
                        {domain}
                      </option>
                    ))}
                    <option value={CUSTOM_DOMAIN}>Other (type your own)</option>
                  </select>
                </label>
              </div>

              {isCustomDomain && (
                <label className="field">
                  <span>Custom Project Domain *</span>
                  <input
                    type="text"
                    maxLength={40}
                    value={form.otherCategory}
                    onChange={(e) => update('otherCategory', e.target.value)}
                    placeholder="Enter your project domain"
                  />
                  {errors.category && <small className="field-error">{errors.category}</small>}
                </label>
              )}

              <label className="field">
                <span>Project Year</span>
                <select value={form.year} onChange={(e) => update('year', e.target.value)}>
                  {PROJECT_YEARS.map((y) => (
                    <option key={y} value={y}>
                      {y}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                <span>Academic Year *</span>
                <select
                  value={form.academicYear}
                  onChange={(e) => update('academicYear', e.target.value)}
                  aria-invalid={Boolean(errors.academicYear)}
                >
                  <option value="">Select academic year</option>
                  {ACADEMIC_YEARS.map((academicYear) => (
                    <option key={academicYear} value={academicYear}>
                      {academicYear}
                    </option>
                  ))}
                </select>
                {errors.academicYear && (
                  <small className="field-error">{errors.academicYear}</small>
                )}
              </label>
            </div>
          )}

          {step === 2 && (
            <div className="form-step">
              <h2 className="step-title">Team &amp; technology</h2>

              <label className="field">
                <span>Technologies Used *</span>
                <input
                  type="text"
                  value={form.technologies}
                  onChange={(e) => update('technologies', e.target.value)}
                  placeholder="Python, TensorFlow, Flask, MySQL"
                />
                <small className="hint">Separate technologies with commas</small>
                {errors.technologies && (
                  <small className="field-error">{errors.technologies}</small>
                )}
              </label>

              <div className="field">
                <span>Team Members (max 5)</span>
                <div className="member-inputs">
                  {form.teamMembers.map((m, index) => (
                    <div key={index} className="member-row">
                      <input
                        type="text"
                        value={m.name}
                        onChange={(e) => updateMember(index, 'name', e.target.value)}
                        placeholder="Full name"
                      />
                      <input
                        type="text"
                        className="mono"
                        value={m.rollNo}
                        onChange={(e) => updateMember(index, 'rollNo', e.target.value)}
                        placeholder="Roll no."
                      />
                      {index > 0 && (
                        <button
                          type="button"
                          className="remove-btn"
                          onClick={() => removeMember(index)}
                          aria-label="Remove member"
                        >
                          ✕
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {form.teamMembers.length < 5 && (
                  <button type="button" className="link-navy" onClick={addMember}>
                    + Add team member
                  </button>
                )}
                {errors.teamMembers && (
                  <small className="field-error">{errors.teamMembers}</small>
                )}
              </div>
            </div>
          )}

          {step === 3 && (
            <div className="form-step">
              <h2 className="step-title">Cover image</h2>
              <p className="muted small">
                Choose an image for {isCustomDomain ? 'your domain' : form.category}, or upload your own.
              </p>

              <div className="image-picker">
                {getCoverOptions(form.department, isCustomDomain ? form.otherCategory : form.category).map((cover) => (
                  <button
                    type="button"
                    key={cover.id}
                    className={form.image === cover.id && !coverFile ? 'image-option selected' : 'image-option'}
                    onClick={() => selectBuiltInCover(cover.id)}
                    aria-label={`Select ${cover.label} cover`}
                  >
                    <img src={imageUrl(cover.id, 300, 150)} alt={cover.label} />
                    <span>{cover.label}</span>
                  </button>
                ))}
              </div>

              <label className="field cover-upload-field">
                <span>Upload your own cover image (optional)</span>
                <input type="file" accept="image/jpeg,image/png" onChange={handleCoverUpload} />
                <small className="hint">
                  JPEG or PNG, exactly 1200 × 630 pixels, maximum 5 MB.
                </small>
                {coverError && <small className="field-error">{coverError}</small>}
              </label>

              <div className="preview-box">
                <strong className="preview-label">SUBMISSION PREVIEW</strong>
                <div className="preview-body">
                  <img src={coverPreview || imageUrl(form.image, 200, 120)} alt="Cover preview" />
                  <div>
                    <strong>{form.title || 'Your project title'}</strong>
                    <p className="muted small">
                      {form.department} · {isCustomDomain ? form.otherCategory || 'Other' : form.category} · {form.year}
                    </p>
                    <p className="muted small">
                      {form.teamMembers
                        .filter((m) => m.name.trim())
                        .map((m) => m.name)
                        .join(', ')}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className="form-nav">
            {step > 1 ? (
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setStep((s) => s - 1)}
              >
                ← Previous
              </button>
            ) : (
              <span />
            )}

            {step < 3 ? (
              <button type="button" className="btn btn-navy" onClick={goNext}>
                Next →
              </button>
            ) : (
              <button type="submit" className="btn btn-gold" disabled={submitting}>
                {submitting ? 'Submitting…' : 'Submit Project 🚀'}
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}

async function validateCoverImage(file) {
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    return 'Choose a JPEG or PNG image.';
  }
  if (file.size > 5 * 1024 * 1024) {
    return 'Cover image must be 5 MB or smaller.';
  }

  const objectUrl = URL.createObjectURL(file);
  try {
    const dimensions = await new Promise((resolve, reject) => {
      const image = new Image();
      image.onload = () => resolve({ width: image.naturalWidth, height: image.naturalHeight });
      image.onerror = reject;
      image.src = objectUrl;
    });
    if (dimensions.width !== 1200 || dimensions.height !== 630) {
      return 'Cover image must be exactly 1200 × 630 pixels.';
    }
  } catch {
    return 'The selected image could not be read.';
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
  return '';
}
