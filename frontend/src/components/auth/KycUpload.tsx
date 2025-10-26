// components/auth/KycUpload.tsx
import React, { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import {
    Box,
    Button,
    TextField,
    Typography,
    Paper,
    Stepper,
    Step,
    StepLabel,
    Alert,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    FormHelperText,
    LinearProgress,
    Grid,
    Card,
    CardMedia,
    IconButton,
    Chip,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
} from '@mui/material';
import {
    CloudUpload as UploadIcon,
    CheckCircle as CheckIcon,
    Cancel as CancelIcon,
    Delete as DeleteIcon,
    Description as DocumentIcon,
    CameraAlt as CameraIcon,
    Info as InfoIcon,
} from '@mui/icons-material';
import { authService } from '../../services/authService';
import { useNotification } from '../../hooks/useNotification';
import { format } from 'date-fns';

// Validation schema
const schema = yup.object({
    documentType: yup
        .string()
        .oneOf(['PASSPORT', 'DRIVERS_LICENSE', 'NATIONAL_ID'])
        .required('Document type is required'),
    documentNumber: yup
        .string()
        .min(5, 'Document number must be at least 5 characters')
        .required('Document number is required'),
    expiryDate: yup
        .date()
        .min(new Date(), 'Document must not be expired')
        .required('Expiry date is required'),
    country: yup.string().required('Country is required'),
}).required();

interface KycFormData {
    documentType: 'PASSPORT' | 'DRIVERS_LICENSE' | 'NATIONAL_ID';
    documentNumber: string;
    expiryDate: Date;
    country: string;
}

interface UploadedFile {
    file: File;
    preview: string;
    type: 'front' | 'back' | 'selfie';
}

const steps = ['Document Information', 'Upload Documents', 'Selfie Verification', 'Review & Submit'];

const countries = [
    'United States',
    'United Kingdom',
    'Canada',
    'Australia',
    'Germany',
    'France',
    'Japan',
    'India',
    'Brazil',
    'South Africa',
    // Add more countries as needed
];

export const KycUpload: React.FC = () => {
    const { showSuccess, showError, showInfo } = useNotification();
    const [activeStep, setActiveStep] = useState(0);
    const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [kycStatus, setKycStatus] = useState<string | null>(null);

    const {
        register,
        handleSubmit,
        control,
        formState: { errors },
        trigger,
        watch,
    } = useForm<KycFormData>({
        resolver: yupResolver(schema),
        defaultValues: {
            documentType: 'PASSPORT',
            documentNumber: '',
            expiryDate: new Date(),
            country: '',
        },
    });

    const documentType = watch('documentType');

    const handleNext = async () => {
        if (activeStep === 0) {
            const isValid = await trigger();
            if (!isValid) return;
        } else if (activeStep === 1) {
            const frontDoc = uploadedFiles.find(f => f.type === 'front');
            if (!frontDoc) {
                showError('Please upload the front of your document');
                return;
            }
            if (documentType !== 'PASSPORT') {
                const backDoc = uploadedFiles.find(f => f.type === 'back');
                if (!backDoc) {
                    showError('Please upload the back of your document');
                    return;
                }
            }
        } else if (activeStep === 2) {
            const selfie = uploadedFiles.find(f => f.type === 'selfie');
            if (!selfie) {
                showError('Please upload a selfie for verification');
                return;
            }
        }
        setActiveStep(prev => prev + 1);
    };

    const handleBack = () => {
        setActiveStep(prev => prev - 1);
    };

    const onDrop = useCallback((acceptedFiles: File[], fileType: 'front' | 'back' | 'selfie') => {
        const file = acceptedFiles[0];
        if (file) {
            const preview = URL.createObjectURL(file);
            setUploadedFiles(prev => [
                ...prev.filter(f => f.type !== fileType),
                { file, preview, type: fileType }
            ]);
            showSuccess(`${fileType.charAt(0).toUpperCase() + fileType.slice(1)} document uploaded successfully`);
        }
    }, [showSuccess]);

    const removeFile = (fileType: string) => {
        setUploadedFiles(prev => {
            const filtered = prev.filter(f => f.type !== fileType);
            // Clean up the preview URL
            const removed = prev.find(f => f.type === fileType);
            if (removed) {
                URL.revokeObjectURL(removed.preview);
            }
            return filtered;
        });
    };

    const { getRootProps: getFrontProps, getInputProps: getFrontInput, isDragActive: isFrontDragActive } = useDropzone({
        onDrop: (files) => onDrop(files, 'front'),
        accept: {
            'image/*': ['.jpeg', '.jpg', '.png', '.webp']
        },
        maxFiles: 1,
        maxSize: 5242880, // 5MB
    });

    const { getRootProps: getBackProps, getInputProps: getBackInput, isDragActive: isBackDragActive } = useDropzone({
        onDrop: (files) => onDrop(files, 'back'),
        accept: {
            'image/*': ['.jpeg', '.jpg', '.png', '.webp']
        },
        maxFiles: 1,
        maxSize: 5242880, // 5MB
    });

    const { getRootProps: getSelfieProps, getInputProps: getSelfieInput, isDragActive: isSelfieDragActive } = useDropzone({
        onDrop: (files) => onDrop(files, 'selfie'),
        accept: {
            'image/*': ['.jpeg', '.jpg', '.png', '.webp']
        },
        maxFiles: 1,
        maxSize: 5242880, // 5MB
    });

    const onSubmit = async (data: KycFormData) => {
        setIsSubmitting(true);
        try {
            const frontFile = uploadedFiles.find(f => f.type === 'front');
            const backFile = uploadedFiles.find(f => f.type === 'back');
            const selfieFile = uploadedFiles.find(f => f.type === 'selfie');

            if (!frontFile || !selfieFile) {
                showError('Required files are missing');
                return;
            }

            const kycData = {
                ...data,
                expiryDate: format(data.expiryDate, 'yyyy-MM-dd'),
                frontImage: frontFile.file,
                backImage: backFile?.file,
                selfieImage: selfieFile.file,
            };

            await authService.uploadKycDocuments(kycData, (progress) => {
                setUploadProgress(progress);
            });

            showSuccess('KYC documents submitted successfully!', 'Verification Pending');
            setKycStatus('PENDING');

            // Clean up preview URLs
            uploadedFiles.forEach(f => URL.revokeObjectURL(f.preview));

        } catch (error: any) {
            showError(error.message || 'Failed to submit KYC documents');
        } finally {
            setIsSubmitting(false);
            setUploadProgress(0);
        }
    };

    const getStepContent = (step: number) => {
        switch (step) {
            case 0:
                return (
                    <Box>
                        <Typography variant="h6" gutterBottom>
                            Document Information
                        </Typography>
                        <Typography variant="body2" color="text.secondary" paragraph>
                            Please provide information about your identity document
                        </Typography>

                        <Grid container spacing={3}>
                            <Grid item xs={12}>
                                <FormControl fullWidth error={!!errors.documentType}>
                                    <InputLabel>Document Type</InputLabel>
                                    <Controller
                                        name="documentType"
                                        control={control}
                                        render={({ field }) => (
                                            <Select {...field} label="Document Type">
                                                <MenuItem value="PASSPORT">Passport</MenuItem>
                                                <MenuItem value="DRIVERS_LICENSE">Driver's License</MenuItem>
                                                <MenuItem value="NATIONAL_ID">National ID Card</MenuItem>
                                            </Select>
                                        )}
                                    />
                                    {errors.documentType && (
                                        <FormHelperText>{errors.documentType.message}</FormHelperText>
                                    )}
                                </FormControl>
                            </Grid>

                            <Grid item xs={12}>
                                <TextField
                                    {...register('documentNumber')}
                                    label="Document Number"
                                    fullWidth
                                    error={!!errors.documentNumber}
                                    helperText={errors.documentNumber?.message}
                                />
                            </Grid>

                            <Grid item xs={12}>
                                <TextField
                                    {...register('expiryDate')}
                                    label="Expiry Date"
                                    type="date"
                                    fullWidth
                                    InputLabelProps={{ shrink: true }}
                                    error={!!errors.expiryDate}
                                    helperText={errors.expiryDate?.message}
                                />
                            </Grid>

                            <Grid item xs={12}>
                                <FormControl fullWidth error={!!errors.country}>
                                    <InputLabel>Country</InputLabel>
                                    <Controller
                                        name="country"
                                        control={control}
                                        render={({ field }) => (
                                            <Select {...field} label="Country">
                                                {countries.map(country => (
                                                    <MenuItem key={country} value={country}>
                                                        {country}
                                                    </MenuItem>
                                                ))}
                                            </Select>
                                        )}
                                    />
                                    {errors.country && (
                                        <FormHelperText>{errors.country.message}</FormHelperText>
                                    )}
                                </FormControl>
                            </Grid>
                        </Grid>
                    </Box>
                );

            case 1:
                const frontDoc = uploadedFiles.find(f => f.type === 'front');
                const backDoc = uploadedFiles.find(f => f.type === 'back');

                return (
                    <Box>
                        <Typography variant="h6" gutterBottom>
                            Upload Documents
                        </Typography>
                        <Typography variant="body2" color="text.secondary" paragraph>
                            Upload clear photos of your {documentType === 'PASSPORT' ? 'passport' : 'ID document'}
                        </Typography>

                        <Alert severity="info" sx={{ mb: 3 }}>
                            <Typography variant="body2">
                                • Ensure all text is clearly visible and readable<br />
                                • Document should fill most of the frame<br />
                                • Avoid glare and shadows<br />
                                • Maximum file size: 5MB per image
                            </Typography>
                        </Alert>

                        <Grid container spacing={3}>
                            <Grid item xs={12} md={documentType === 'PASSPORT' ? 12 : 6}>
                                <Paper
                                    {...getFrontProps()}
                                    sx={{
                                        p: 3,
                                        textAlign: 'center',
                                        backgroundColor: isFrontDragActive ? 'action.hover' : 'background.paper',
                                        border: '2px dashed',
                                        borderColor: frontDoc ? 'success.main' : 'divider',
                                        cursor: 'pointer',
                                        transition: 'all 0.3s',
                                        '&:hover': {
                                            borderColor: 'primary.main',
                                            backgroundColor: 'action.hover',
                                        },
                                    }}
                                >
                                    <input {...getFrontInput()} />
                                    {frontDoc ? (
                                        <Box>
                                            <CardMedia
                                                component="img"
                                                src={frontDoc.preview}
                                                sx={{ height: 200, objectFit: 'contain', mb: 2 }}
                                            />
                                            <Chip
                                                label="Front uploaded"
                                                color="success"
                                                onDelete={() => removeFile('front')}
                                                deleteIcon={<DeleteIcon />}
                                            />
                                        </Box>
                                    ) : (
                                        <Box>
                                            <UploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                                            <Typography variant="h6" gutterBottom>
                                                {documentType === 'PASSPORT' ? 'Passport Page' : 'Front of Document'}
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary">
                                                Drag & drop or click to upload
                                            </Typography>
                                        </Box>
                                    )}
                                </Paper>
                            </Grid>

                            {documentType !== 'PASSPORT' && (
                                <Grid item xs={12} md={6}>
                                    <Paper
                                        {...getBackProps()}
                                        sx={{
                                            p: 3,
                                            textAlign: 'center',
                                            backgroundColor: isBackDragActive ? 'action.hover' : 'background.paper',
                                            border: '2px dashed',
                                            borderColor: backDoc ? 'success.main' : 'divider',
                                            cursor: 'pointer',
                                            transition: 'all 0.3s',
                                            '&:hover': {
                                                borderColor: 'primary.main',
                                                backgroundColor: 'action.hover',
                                            },
                                        }}
                                    >
                                        <input {...getBackInput()} />
                                        {backDoc ? (
                                            <Box>
                                                <CardMedia
                                                    component="img"
                                                    src={backDoc.preview}
                                                    sx={{ height: 200, objectFit: 'contain', mb: 2 }}
                                                />
                                                <Chip
                                                    label="Back uploaded"
                                                    color="success"
                                                    onDelete={() => removeFile('back')}
                                                    deleteIcon={<DeleteIcon />}
                                                />
                                            </Box>
                                        ) : (
                                            <Box>
                                                <UploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                                                <Typography variant="h6" gutterBottom>
                                                    Back of Document
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    Drag & drop or click to upload
                                                </Typography>
                                            </Box>
                                        )}
                                    </Paper>
                                </Grid>
                            )}
                        </Grid>
                    </Box>
                );

            case 2:
                const selfie = uploadedFiles.find(f => f.type === 'selfie');

                return (
                    <Box>
                        <Typography variant="h6" gutterBottom>
                            Selfie Verification
                        </Typography>
                        <Typography variant="body2" color="text.secondary" paragraph>
                            Take a clear selfie for identity verification
                        </Typography>

                        <Alert severity="warning" sx={{ mb: 3 }}>
                            <Typography variant="body2">
                                • Face the camera directly<br />
                                • Ensure good lighting on your face<br />
                                • Remove glasses if possible<br />
                                • Keep a neutral expression<br />
                                • Hold your document next to your face (optional but recommended)
                            </Typography>
                        </Alert>

                        <Paper
                            {...getSelfieProps()}
                            sx={{
                                p: 4,
                                textAlign: 'center',
                                backgroundColor: isSelfieDragActive ? 'action.hover' : 'background.paper',
                                border: '2px dashed',
                                borderColor: selfie ? 'success.main' : 'divider',
                                cursor: 'pointer',
                                maxWidth: 500,
                                mx: 'auto',
                                transition: 'all 0.3s',
                                '&:hover': {
                                    borderColor: 'primary.main',
                                    backgroundColor: 'action.hover',
                                },
                            }}
                        >
                            <input {...getSelfieInput()} />
                            {selfie ? (
                                <Box>
                                    <CardMedia
                                        component="img"
                                        src={selfie.preview}
                                        sx={{ height: 300, objectFit: 'contain', mb: 2 }}
                                    />
                                    <Chip
                                        label="Selfie uploaded"
                                        color="success"
                                        onDelete={() => removeFile('selfie')}
                                        deleteIcon={<DeleteIcon />}
                                    />
                                </Box>
                            ) : (
                                <Box>
                                    <CameraIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
                                    <Typography variant="h6" gutterBottom>
                                        Upload Selfie
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Drag & drop or click to upload
                                    </Typography>
                                </Box>
                            )}
                        </Paper>
                    </Box>
                );

            case 3:
                return (
                    <Box>
                        <Typography variant="h6" gutterBottom>
                            Review & Submit
                        </Typography>
                        <Typography variant="body2" color="text.secondary" paragraph>
                            Please review your information before submitting
                        </Typography>

                        <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
                            <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                                Document Information
                            </Typography>
                            <List dense>
                                <ListItem>
                                    <ListItemText primary="Document Type" secondary={documentType} />
                                </ListItem>
                                <ListItem>
                                    <ListItemText primary="Document Number" secondary={watch('documentNumber')} />
                                </ListItem>
                                <ListItem>
                                    <ListItemText primary="Country" secondary={watch('country')} />
                                </ListItem>
                                <ListItem>
                                    <ListItemText
                                        primary="Expiry Date"
                                        secondary={format(watch('expiryDate'), 'MMMM dd, yyyy')}
                                    />
                                </ListItem>
                            </List>
                        </Paper>

                        <Paper elevation={2} sx={{ p: 3 }}>
                            <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                                Uploaded Documents
                            </Typography>
                            <List dense>
                                {uploadedFiles.map((file) => (
                                    <ListItem key={file.type}>
                                        <ListItemIcon>
                                            <CheckIcon color="success" />
                                        </ListItemIcon>
                                        <ListItemText
                                            primary={`${file.type.charAt(0).toUpperCase() + file.type.slice(1)} Document`}
                                            secondary={`${(file.file.size / 1024 / 1024).toFixed(2)} MB`}
                                        />
                                    </ListItem>
                                ))}
                            </List>
                        </Paper>

                        {uploadProgress > 0 && (
                            <Box sx={{ mt: 3 }}>
                                <Typography variant="body2" color="text.secondary" gutterBottom>
                                    Uploading documents...
                                </Typography>
                                <LinearProgress variant="determinate" value={uploadProgress} />
                            </Box>
                        )}

                        <Alert severity="info" sx={{ mt: 3 }}>
                            <Typography variant="body2">
                                By submitting, you confirm that all information provided is accurate and authentic.
                                Verification typically takes 1-3 business days.
                            </Typography>
                        </Alert>
                    </Box>
                );

            default:
                return null;
        }
    };

    if (kycStatus === 'PENDING') {
        return (
            <Paper elevation={3} sx={{ p: 4, maxWidth: 600, mx: 'auto', textAlign: 'center' }}>
                <CheckIcon sx={{ fontSize: 64, color: 'success.main', mb: 2 }} />
                <Typography variant="h5" gutterBottom fontWeight="bold">
                    KYC Documents Submitted Successfully
                </Typography>
                <Typography variant="body1" color="text.secondary" paragraph>
                    Your documents are under review. We'll notify you once the verification is complete.
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Verification typically takes 1-3 business days.
                </Typography>
            </Paper>
        );
    }

    return (
        <Paper elevation={3} sx={{ p: 4, maxWidth: 800, mx: 'auto' }}>
            <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
                {steps.map((label) => (
                    <Step key={label}>
                        <StepLabel>{label}</StepLabel>
                    </Step>
                ))}
            </Stepper>

            <form onSubmit={handleSubmit(onSubmit)}>
                {getStepContent(activeStep)}

                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4 }}>
                    <Button
                        disabled={activeStep === 0 || isSubmitting}
                        onClick={handleBack}
                        sx={{ mr: 1 }}
                    >
                        Back
                    </Button>
                    {activeStep === steps.length - 1 ? (
                        <Button
                            type="submit"
                            variant="contained"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Submitting...' : 'Submit for Verification'}
                        </Button>
                    ) : (
                        <Button variant="contained" onClick={handleNext}>
                            Next
                        </Button>
                    )}
                </Box>
            </form>
        </Paper>
    );
};


